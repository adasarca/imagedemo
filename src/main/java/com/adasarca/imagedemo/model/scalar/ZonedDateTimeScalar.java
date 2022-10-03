package com.adasarca.imagedemo.model.scalar;

import com.netflix.graphql.dgs.DgsScalar;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@DgsScalar(name="ZonedDateTime")
public class ZonedDateTimeScalar implements Coercing<ZonedDateTime, String> {

    @Override
    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
        if (dataFetcherResult instanceof ZonedDateTime) {
            return ((ZonedDateTime) dataFetcherResult).format(DateTimeFormatter.ISO_DATE_TIME);
        } else {
            throw new CoercingSerializeException("Not a valid ZonedDateTime");
        }
    }

    @Override
    public ZonedDateTime parseValue(Object input) throws CoercingParseValueException {
        return ZonedDateTime.parse(input.toString(), DateTimeFormatter.ISO_DATE_TIME);
    }

    @Override
    public ZonedDateTime parseLiteral(Object input) throws CoercingParseLiteralException {
        if (input instanceof StringValue) {
            return ZonedDateTime.parse(((StringValue) input).getValue(), DateTimeFormatter.ISO_DATE_TIME);
        }

        throw new CoercingParseLiteralException("Value is not a valid ISO date time");
    }
}
