package org.embulk.filter.build_json;

import org.embulk.spi.Column;
import org.embulk.spi.PageReader;
import org.embulk.spi.Schema;
import org.embulk.spi.type.Type;
import org.embulk.spi.type.Types;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildJsonParseTreeVisitor
        extends JSONBaseVisitor<Value>
{
    private PageReader pageReader;
    private Schema inputSchema;

    public BuildJsonParseTreeVisitor(Schema inputSchema, PageReader pageReader)
    {
        this.pageReader = pageReader;
        this.inputSchema = inputSchema;
    }

    @Override
    public Value visitObjectWithPair(JSONParser.ObjectWithPairContext ctx)
    {
        Map<Value, Value> map = new HashMap<>();

        for (JSONParser.PairContext pair : ctx.pair()) {
            Value key = ValueFactory.newString(trimQuote(pair.STRING().getText()));
            Value value = visit(pair.value());
            map.put(key, value);
        }
        return ValueFactory.newMap(map);
    }

    @Override
    public Value visitEmptyObject(JSONParser.EmptyObjectContext ctx)
    {
        Map<Value, Value> map = new HashMap<>();
        return ValueFactory.newMap(map);
    }

    @Override
    public Value visitArrayWithValue(JSONParser.ArrayWithValueContext ctx)
    {
        List<Value> list = new ArrayList<>();
        for (JSONParser.ValueContext val_ctx : ctx.value()) {
            Value value = visit(val_ctx);
            list.add(value);
        }
        return ValueFactory.newArray(list);
    }

    @Override
    public Value visitEmptyArray(JSONParser.EmptyArrayContext ctx)
    {
        List<Value> list = new ArrayList<>();
        return ValueFactory.newArray(list);
    }

    @Override
    public Value visitStringValue(JSONParser.StringValueContext ctx)
    {
        String key = trimQuote(ctx.STRING().getText());
        return ValueFactory.newString(key);
    }

    @Override
    public Value visitNumberValue(JSONParser.NumberValueContext ctx)
    {
        Double num = Double.parseDouble(ctx.getText());
        return ValueFactory.newFloat(num);
    }

    @Override
    public Value visitTrueValue(JSONParser.TrueValueContext ctx)
    {
        return ValueFactory.newBoolean(true);
    }

    @Override
    public Value visitFalseValue(JSONParser.FalseValueContext ctx)
    {
        return ValueFactory.newBoolean(false);
    }

    @Override
    public Value visitNullValue(JSONParser.NullValueContext ctx)
    {
        return ValueFactory.newNil();
    }

    @Override
    public Value visitReferenceValue(JSONParser.ReferenceValueContext ctx)
    {
        JSONParser.ReferenceContext ref = ctx.reference();
        String key = ref.ID().getText();
        Column column = inputSchema.lookupColumn(key);
        Value value;
        Type column_type = column.getType();

        if (pageReader.isNull(column)){
            value =  ValueFactory.newNil();
        }
        else if (column_type == Types.BOOLEAN) {
            value = ValueFactory.newBoolean(pageReader.getBoolean(column));
        }
        else if (column_type == Types.DOUBLE) {
            value = ValueFactory.newFloat(pageReader.getDouble(column));
        }
        else if (column_type == Types.STRING) {
            value = ValueFactory.newString(pageReader.getString(column));
        }
        else if (column_type == Types.LONG) {
            value = ValueFactory.newInteger(pageReader.getLong(column));
        }
        else if (column_type == Types.JSON) {
            value = pageReader.getJson(column);
        }
        else {
            // Unsupported type;
            value = ValueFactory.newNil();
        }

        return value;
    }
    private String trimQuote(String str){
        return str.substring(1, str.length() - 1);
    }
}
