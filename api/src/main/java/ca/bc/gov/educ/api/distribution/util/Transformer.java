package ca.bc.gov.educ.api.distribution.util;

import java.io.InputStream;

public interface Transformer {

    public Object unmarshall(byte[] input, Class<?> clazz);

    public Object unmarshall(String input, Class<?> clazz);

    public Object unmarshall(InputStream input, Class<?> clazz);

    public String marshall(Object input);

    public String getAccept();

    public String getContentType();
}
