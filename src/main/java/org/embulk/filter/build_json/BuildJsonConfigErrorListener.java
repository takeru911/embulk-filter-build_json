package org.embulk.filter.build_json;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.embulk.config.ConfigException;

import static java.util.Locale.ENGLISH;

public class BuildJsonConfigErrorListener
        extends BaseErrorListener

{
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
            Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
    {
        String err = String.format(ENGLISH, "Invalid json template. line: %d error: %s", line, msg);
        throw new ConfigException(err);
    }

}
