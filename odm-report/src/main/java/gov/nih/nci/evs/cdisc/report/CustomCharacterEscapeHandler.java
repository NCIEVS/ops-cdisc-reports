package gov.nih.nci.evs.cdisc.report;

import org.glassfish.jaxb.core.marshaller.CharacterEscapeHandler;

import java.io.IOException;
import java.io.Writer;

public class CustomCharacterEscapeHandler implements CharacterEscapeHandler {

    public CustomCharacterEscapeHandler() {
        super();
    }

    public void escape(char[] ch, int start, int length, boolean isAttVal, Writer out) throws IOException {
        // avoid calling the Writerwrite method too much by assuming
        // that the escaping occurs rarely.
        // profiling revealed that this is faster than the naive code.
        int limit = start+length;
        for (int i = start; i < limit; i++) {
            char c = ch[i];
            switch (c) {
                case '<':
                    out.write("&lt;");
                    break;
                case '>':
                    out.write("&gt;");
                    break;
                case '\"':
                    out.write("&quot;");
                    break;
                case '&':
                    out.write("&amp;");
                    break;
                case '\'':
                    out.write("&apos;");
                    break;
                default:
                    if (c > 0x7e) {
                        out.write("&#" + ((int) c) + ";");
                    } else {
                        out.write(c);
                    }
            }
        }

        if( start!=limit )
            out.write(ch,start,limit-start);
    }
}