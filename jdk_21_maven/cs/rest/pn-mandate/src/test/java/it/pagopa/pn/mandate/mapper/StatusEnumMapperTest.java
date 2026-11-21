package it.pagopa.pn.mandate.mapper;

import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.fail;

class StatusEnumMapperTest {

    @Test
    void intValfromStatusActive() {
        //Given
        MandateDto.StatusEnum state = MandateDto.StatusEnum.ACTIVE;

        //When
        int result = StatusEnumMapper.intValfromStatus(state);

        //Then
        Assertions.assertEquals( 20, result);
    }


    @Test
    void intValfromStatusMissingValues() {
        //Given
        MandateDto.StatusEnum[] array = MandateDto.StatusEnum.values();

        //When
        int length = array.length;

        //Then
        Assertions.assertEquals( 5, length);
    }

    @Test
    void intValfromValueConstActive() {
        //Given
        String state = MandateDto.StatusEnum.ACTIVE.getValue();

        //When
        int result = StatusEnumMapper.intValfromValueConst(state);

        //Then
        Assertions.assertEquals( 20, result);
    }

    @Test
    void fromValue() {
        //Given
        int state = 20;

        //When
        MandateDto.StatusEnum result = StatusEnumMapper.fromValue(state);

        //Then
        Assertions.assertEquals( MandateDto.StatusEnum.ACTIVE, result);
    }

    @Test
    void fromValueRejected() {
        //Given
        int state = 30;

        //When
        MandateDto.StatusEnum result = StatusEnumMapper.fromValue(state);

        //Then
        Assertions.assertEquals( MandateDto.StatusEnum.REJECTED, result);
    }

    @Test
    void fromValueRevoked() {
        //Given
        int state = 40;

        //When
        MandateDto.StatusEnum result = StatusEnumMapper.fromValue(state);

        //Then
        Assertions.assertEquals( MandateDto.StatusEnum.REVOKED, result);
    }

    @Test
    void fromValueExpired() {
        //Given
        int state = 50;

        //When
        MandateDto.StatusEnum result = StatusEnumMapper.fromValue(state);

        //Then
        Assertions.assertEquals( MandateDto.StatusEnum.EXPIRED, result);
    }


    @Test
    void fromValueInexistente() {
        //Given
        int state = 0;

        //When
        try {
            StatusEnumMapper.fromValue(state);
            fail("no NoSuchElementException thrown");
        } catch (NoSuchElementException e) {
            // expected
        }

        //Then
    }

    @Test
    void fromValueConstNull() {
        //Given
        String state = null;

        //When
        try {
            StatusEnumMapper.fromValueConst(state);
            fail("no NoSuchElementException thrown");
        } catch (Exception e) {
            // expected
        }

        //Then
    }
}