package com.zaheudev.ctm.service;

import com.zaheudev.ctm.entity.CardTokenEntity;
import com.zaheudev.ctm.repository.CardTokenRepository;
import com.zaheudev.shared.avro.PaymentMethodEnum;
import com.zaheudev.shared.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class CardTokenServiceTest {

    @Mock
    private CardTokenRepository cardTokenRepository;

    @Mock
    private Generator generator;

    private CardTokenService cardTokenService;

    @BeforeEach
    void setUp() throws Exception {
        cardTokenService = new CardTokenService(cardTokenRepository, generator);
        ReflectionTestUtils.setField(cardTokenService, "secretKeyString", "1234567890123456");
        lenient().when(generator.generatePAN(16)).thenReturn("9999999999999999");
        lenient().when(generator.encryptPAN(any(), any())).thenReturn("iv:encrypted");
    }

    @Test
    void tokenizeShouldDetectVisaNetwork() throws Exception {
        TokenizeRequest request = TokenizeRequest.builder()
                .cardNumber("4111111111111111")
                .cvv("123")
                .expiryMonth("12")
                .expiryYear("2027")
                .cardHolderName("John Doe")
                .build();

        TokenizeResponse response = cardTokenService.tokenize(request);

        assertThat(response.getCardNetwork()).isEqualTo(PaymentMethodEnum.VISA);
        assertThat(response.getBin()).isEqualTo("411111");
        assertThat(response.getLastFour()).isEqualTo("1111");
        verify(cardTokenRepository).save(any(CardTokenEntity.class));
    }

    @Test
    void tokenizeShouldDetectMastercardNetwork() throws Exception {
        TokenizeRequest request = TokenizeRequest.builder()
                .cardNumber("5111111111111111").cvv("123")
                .expiryMonth("12").expiryYear("2027").cardHolderName("John Doe").build();

        TokenizeResponse response = cardTokenService.tokenize(request);

        assertThat(response.getCardNetwork()).isEqualTo(PaymentMethodEnum.MASTERCARD);
    }

    @Test
    void tokenizeShouldDetectAmexNetwork() throws Exception {
        TokenizeRequest request = TokenizeRequest.builder()
                .cardNumber("341111111111111").cvv("123")
                .expiryMonth("12").expiryYear("2027").cardHolderName("John Doe").build();

        TokenizeResponse response = cardTokenService.tokenize(request);

        assertThat(response.getCardNetwork()).isEqualTo(PaymentMethodEnum.AMEX);
    }

    @Test
    void tokenizeShouldDetectDiscoverNetwork() throws Exception {
        TokenizeRequest request = TokenizeRequest.builder()
                .cardNumber("6011111111111117").cvv("123")
                .expiryMonth("12").expiryYear("2027").cardHolderName("John Doe").build();

        TokenizeResponse response = cardTokenService.tokenize(request);

        assertThat(response.getCardNetwork()).isEqualTo(PaymentMethodEnum.DISCOVER);
    }

    @Test
    void tokenizeShouldDefaultToVisaForUnknownPrefix() throws Exception {
        TokenizeRequest request = TokenizeRequest.builder()
                .cardNumber("9999999999999999").cvv("123")
                .expiryMonth("12").expiryYear("2027").cardHolderName("John Doe").build();

        TokenizeResponse response = cardTokenService.tokenize(request);

        assertThat(response.getCardNetwork()).isEqualTo(PaymentMethodEnum.VISA);
    }

    @Test
    void tokenizeShouldSetDebitFor453Bin() throws Exception {
        TokenizeRequest request = TokenizeRequest.builder()
                .cardNumber("4530001111111111").cvv("123")
                .expiryMonth("12").expiryYear("2027").cardHolderName("John Doe").build();

        TokenizeResponse response = cardTokenService.tokenize(request);

        assertThat(response.getCardType()).isEqualTo("DEBIT");
    }

    @Test
    void tokenizeShouldSetDebitFor520Bin() throws Exception {
        TokenizeRequest request = TokenizeRequest.builder()
                .cardNumber("5200001111111111").cvv("123")
                .expiryMonth("12").expiryYear("2027").cardHolderName("John Doe").build();

        TokenizeResponse response = cardTokenService.tokenize(request);

        assertThat(response.getCardType()).isEqualTo("DEBIT");
    }

    @Test
    void tokenizeShouldSetCreditForOtherBins() throws Exception {
        TokenizeRequest request = TokenizeRequest.builder()
                .cardNumber("4111111111111111").cvv("123")
                .expiryMonth("12").expiryYear("2027").cardHolderName("John Doe").build();

        TokenizeResponse response = cardTokenService.tokenize(request);

        assertThat(response.getCardType()).isEqualTo("CREDIT");
    }

    @Test
    void tokenizeShouldGenerateTokenRefWithTknPrefix() throws Exception {
        TokenizeRequest request = TokenizeRequest.builder()
                .cardNumber("4111111111111111").cvv("123")
                .expiryMonth("12").expiryYear("2027").cardHolderName("John Doe").build();

        TokenizeResponse response = cardTokenService.tokenize(request);

        assertThat(response.getTokenRef()).startsWith("TKN-");
    }

    @Test
    void detokenizeShouldDecryptAndReturnCardNumber() throws Exception {
        CardTokenEntity entity = CardTokenEntity.builder()
                .tokenRef("TKN-abc").encryptedPan("iv:enc").expiryMonth("12").expiryYear("2027").build();
        when(cardTokenRepository.findByTokenRef("TKN-abc")).thenReturn(Optional.of(entity));
        when(generator.decryptPAN(any(), any())).thenReturn("4111111111111111");

        DetokenizeResponse response = cardTokenService.detokenize("TKN-abc");

        assertThat(response.getCardNumber()).isEqualTo("4111111111111111");
    }

    @Test
    void detokenizeShouldThrowWhenTokenNotFound() {
        when(cardTokenRepository.findByTokenRef("NONEXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardTokenService.detokenize("NONEXIST"))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getMetadataShouldReturnTokenInfo() {
        CardTokenEntity entity = CardTokenEntity.builder()
                .tokenRef("TKN-abc").bin("411111").lastFour("1111")
                .cardType("CREDIT").cardNetwork(com.zaheudev.shared.avro.PaymentMethodEnum.VISA)
                .cardholderName("John").expiryMonth("12").expiryYear("2027")
                .status(TokenStatus.ACTIVE).build();
        when(cardTokenRepository.findByTokenRef("TKN-abc")).thenReturn(Optional.of(entity));

        CardTokenMetadata metadata = cardTokenService.getMetadata("TKN-abc");

        assertThat(metadata.getBin()).isEqualTo("411111");
        assertThat(metadata.getLastFour()).isEqualTo("1111");
        assertThat(metadata.getCardNetwork()).isEqualTo("VISA");
    }

    @Test
    void getMetadataShouldThrowWhenNotFound() {
        when(cardTokenRepository.findByTokenRef("NONEXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardTokenService.getMetadata("NONEXIST"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getStatusShouldReturnTokenStatus() {
        CardTokenEntity entity = CardTokenEntity.builder()
                .tokenRef("TKN-abc").status(TokenStatus.EXPIRED).build();
        when(cardTokenRepository.findByTokenRef("TKN-abc")).thenReturn(Optional.of(entity));

        TokenStatus status = cardTokenService.getStatus("TKN-abc");

        assertThat(status).isEqualTo(TokenStatus.EXPIRED);
    }

    @Test
    void updateStatusShouldSaveUpdatedStatus() {
        CardTokenEntity entity = CardTokenEntity.builder()
                .tokenRef("TKN-abc").status(TokenStatus.ACTIVE).build();
        when(cardTokenRepository.findByTokenRef("TKN-abc")).thenReturn(Optional.of(entity));

        TokenStatus result = cardTokenService.updateStatus("TKN-abc", TokenStatus.CANCELLED);

        assertThat(result).isEqualTo(TokenStatus.CANCELLED);
        verify(cardTokenRepository).save(entity);
    }
}
