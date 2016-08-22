package org.embulk.filter.build_json;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.Exec;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageReader;
import org.embulk.spi.Schema;
import org.msgpack.value.Value;
import org.slf4j.Logger;

public class BuildJsonVisitorImpl
        implements ColumnVisitor

{
    private static final Logger logger = Exec.getLogger(BuildJsonFilterPlugin.class);
    private final BuildJsonFilterPlugin.PluginTask task;
    private final Schema inputSchema;
    private final Schema outputSchema;
    private final PageReader pageReader;
    private final PageBuilder pageBuilder;
    private ParseTree parse_tree;
    private BuildJsonParseTreeVisitor visitor;

    BuildJsonVisitorImpl(BuildJsonFilterPlugin.PluginTask task, Schema inputSchema, Schema outputSchema, PageReader pageReader, PageBuilder pageBuilder)
    {
        this.task = task;
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
        this.pageReader = pageReader;
        this.pageBuilder = pageBuilder;
        initializeJSONParseTree();
    }

    private void initializeJSONParseTree()
    {
        String template = task.getJsonColumn().getTemplate();
        ANTLRInputStream input = new ANTLRInputStream(template);
        JSONLexer lexer = new JSONLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JSONParser parser = new JSONParser(tokens);
        this.parse_tree = parser.json();
        this.visitor = new BuildJsonParseTreeVisitor(inputSchema, pageReader);
    }

    @Override
    public void booleanColumn(Column outputColumn)
    {
        Column inputColumn = inputSchema.lookupColumn(outputColumn.getName());
        if (pageReader.isNull(inputColumn)) {
            pageBuilder.setNull(outputColumn);
        }
        else {
            pageBuilder.setBoolean(outputColumn, pageReader.getBoolean(inputColumn));
        }
    }

    @Override
    public void longColumn(Column outputColumn)
    {
        Column inputColumn = inputSchema.lookupColumn(outputColumn.getName());
        if (pageReader.isNull(inputColumn)) {
            pageBuilder.setNull(outputColumn);
        }
        else {
            pageBuilder.setLong(outputColumn, pageReader.getLong(inputColumn));
        }
    }

    @Override
    public void doubleColumn(Column outputColumn)
    {
        Column inputColumn = inputSchema.lookupColumn(outputColumn.getName());
        if (pageReader.isNull(inputColumn)) {
            pageBuilder.setNull(outputColumn);
        }
        else {
            pageBuilder.setDouble(outputColumn, pageReader.getDouble(inputColumn));
        }
    }

    @Override
    public void timestampColumn(Column outputColumn)
    {
        Column inputColumn = inputSchema.lookupColumn(outputColumn.getName());
        if (pageReader.isNull(inputColumn)) {
            pageBuilder.setNull(outputColumn);
        }
        else {
            pageBuilder.setTimestamp(outputColumn, pageReader.getTimestamp(inputColumn));
        }
    }

    @Override
    public void stringColumn(Column outputColumn)
    {
        String json_column_name = task.getJsonColumn().getName();
        if (json_column_name.equals(outputColumn.getName())) {
            Value value = visitor.visit(parse_tree);
            if (value.isNilValue()) {
                pageBuilder.setNull(outputColumn);
            }
            else {
                pageBuilder.setString(outputColumn, value.toString());
            }
        }
        else {
            Column inputColumn = inputSchema.lookupColumn(outputColumn.getName());
            if (pageReader.isNull(inputColumn)) {
                pageBuilder.setNull(outputColumn);
            }
            else {
                pageBuilder.setString(outputColumn, pageReader.getString(inputColumn));
            }
        }
    }

    @Override
    public void jsonColumn(Column outputColumn)
    {
        String json_column_name = task.getJsonColumn().getName();
        if (json_column_name.equals(outputColumn.getName())) {
            Value value = visitor.visit(parse_tree);

            if (value.isNilValue()) {
                pageBuilder.setNull(outputColumn);
            }
            else {
                pageBuilder.setJson(outputColumn, value);
            }
        }
        else {
            Column inputColumn = inputSchema.lookupColumn(outputColumn.getName());
            if (pageReader.isNull(inputColumn)) {
                pageBuilder.setNull(outputColumn);
            }
            else {
                pageBuilder.setJson(outputColumn, pageReader.getJson(inputColumn));
            }
        }
    }
}
