package org.embulk.filter.build_json;

import org.embulk.config.ConfigException;
import org.embulk.spi.Column;
import org.embulk.spi.Schema;
import org.embulk.spi.type.Type;
import org.embulk.spi.type.Types;
import org.msgpack.value.Value;

import static java.util.Locale.ENGLISH;

/**
 * Created by hsato on 2016/08/23.
 */
public class BuildJsonParseTreeConfigVisitor
        extends JSONBaseVisitor<Boolean>
{
    private Schema inputSchema;

    public BuildJsonParseTreeConfigVisitor(Schema inputSchema)
    {
        this.inputSchema = inputSchema;
    }

    @Override
    public Boolean visitReferenceValue(JSONParser.ReferenceValueContext ctx)
    {
        JSONParser.ReferenceContext ref = ctx.reference();
        String key = ref.ID().getText();
        inputSchema.lookupColumn(key); // throw ConfigException if column not found.
        return true;
    }


}
