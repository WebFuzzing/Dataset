package it.pagopa.pn.mandate.utils;

import it.pagopa.pn.mandate.exceptions.PnInvalidQrCodeException;
import it.pagopa.pn.mandate.springbootcfg.QrUrlCodecConsumerActivation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AarQrUtilsTest {

    @Test
    void extractQr_Token_shouldReturnDecodedValue() {
        String url= "https://cittadini.dev.notifichedigitali.it/?aar=UURZVS1QVUFELVFNUUEtMjAyMzA1LUctMV9QRi0zNzY1NDU2MS00NDZhLTRjODgtYjMyOC02Njk5YTgzMjJiMzNfYWI5MTAyNGEtYzNmZS00N2U5LThkMzgtNDVhODA0ODU4MzY3";
        QrUrlCodecConsumerActivation mockCodec = mock(QrUrlCodecConsumerActivation.class);
        when(mockCodec.decode(url)).thenReturn("decodedValue");

        AarQrUtils utils = new AarQrUtils(mockCodec);
        String result = utils.extractQrToken(url);

        assertEquals("decodedValue", result);
        verify(mockCodec).decode(url);
    }

    @Test
    void extractQr_shouldThrowPnInvalidQrTokenCodeException() {
        QrUrlCodecConsumerActivation mockCodec = mock(QrUrlCodecConsumerActivation.class);
        when(mockCodec.decode("invalidUrl")).thenThrow(new IllegalArgumentException());

        AarQrUtils utils = new AarQrUtils(mockCodec);

        PnInvalidQrCodeException ex = assertThrows(PnInvalidQrCodeException.class, () -> utils.extractQrToken("invalidUrl"));
        assertEquals("Qr code non valido", ex.getMessage());
    }
}
