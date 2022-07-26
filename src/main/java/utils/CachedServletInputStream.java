package utils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CachedServletInputStream extends ServletInputStream {
    private ByteArrayInputStream input;
    private boolean finished;

    public CachedServletInputStream(ByteArrayOutputStream cachedBytes) {
        finished = true;
        input = new ByteArrayInputStream(cachedBytes.toByteArray());
    }

    @Override
    public int read() throws IOException {
        finished = true;
        return input.read();
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public boolean isReady() {
        return !finished;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        throw new IllegalStateException();
    }
}
