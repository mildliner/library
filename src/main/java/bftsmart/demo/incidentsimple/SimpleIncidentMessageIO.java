package bftsmart.demo.incidentsimple;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class SimpleIncidentMessageIO {

    private SimpleIncidentMessageIO() {
    }

    public static byte[] toBytes(Object value) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);
        objectOut.writeObject(value);
        objectOut.flush();
        return byteOut.toByteArray();
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromBytes(byte[] input, Class<T> type) throws IOException, ClassNotFoundException {
        ObjectInputStream objectIn = new ObjectInputStream(new ByteArrayInputStream(input));
        Object value = objectIn.readObject();
        return (T) value;
    }
}
