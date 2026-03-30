package bftsmart.demo.incident;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public final class IncidentMessageIO {

    private IncidentMessageIO() {
    }

    public static byte[] toBytes(Serializable value) throws IOException {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutputStream objectOut = new ObjectOutputStream(byteOut)) {
            objectOut.writeObject(value);
            objectOut.flush();
            return byteOut.toByteArray();
        }
    }

    public static <T> T fromBytes(byte[] data, Class<T> type) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
             ObjectInputStream objectIn = new ObjectInputStream(byteIn)) {
            Object value = objectIn.readObject();
            return type.cast(value);
        }
    }
}
