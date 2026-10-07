package com.example.demo.auth.model.mapper;

import com.example.demo.auth.model.Token;
import com.example.demo.auth.model.dto.response.TokenResponse;
import com.example.demo.common.model.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Mapper interface for converting {@link Token} to {@link TokenResponse}.
 * This interface extends {@link BaseMapper} and provides the mapping logic from the
 * {@link Token} entity to a {@link TokenResponse} DTO. It includes a method for mapping a
 * {@link Token} to a {@link TokenResponse}.
 */
@Mapper
public interface TokenToTokenResponseMapper extends BaseMapper<Token, TokenResponse> {

    /**
     * Maps a {@link Token} to a {@link TokenResponse}.
     *
     * @param source The {@link Token} object to be mapped.
     * @return A {@link TokenResponse} object containing the mapped data from the {@link Token}.
     */
    @Override
    TokenResponse map(Token source);

    /**
     * Initializes and returns an instance of {@link TokenToTokenResponseMapper}.
     * This method provides a way to retrieve the singleton mapper instance for
     * converting {@link Token} to {@link TokenResponse}.
     *
     * @return An initialized {@link TokenToTokenResponseMapper} instance.
     */
    static TokenToTokenResponseMapper initialize() {
        return Mappers.getMapper(TokenToTokenResponseMapper.class);
    }

}
