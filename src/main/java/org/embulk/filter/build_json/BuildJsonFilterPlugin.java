package org.embulk.filter.build_json;

import com.google.common.base.Optional;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.Column;
import org.embulk.spi.Exec;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.Page;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageOutput;
import org.embulk.spi.PageReader;
import org.embulk.spi.Schema;
import org.embulk.spi.SchemaConfigException;
import org.embulk.spi.type.Type;
import org.embulk.spi.type.Types;
import org.slf4j.Logger;


public class BuildJsonFilterPlugin
        implements FilterPlugin
{

    private static final Logger logger = Exec.getLogger(BuildJsonFilterPlugin.class);
    private static final Type DEFAULT_COLUMN_TYPE = Types.JSON;

    public interface PluginTask
            extends Task
    {
        @Config("column")
        @ConfigDefault("null")
        JsonColumn getJsonColumn();

// TODO
//        @Config("skip_if_null")
//        @ConfigDefault("[]")
//        List<String> getColumnNamesSkipIfNull();

        @Config("default_timezone")
        @ConfigDefault("\"UTC\"")
        String getDefaultTimezone();

        @Config("default_format")
        @ConfigDefault("\"%Y-%m-%d %H:%M:%S.%N %z\"")
        String getDefaultFormat();
    }

    public interface JsonColumn
            extends Task
    {
        @Config("name")
        @ConfigDefault("json_payload")
        String getName();

        @Config("type")
        @ConfigDefault("null")
        Optional<Type> getType();

        @Config("template")
        String getTemplate();
    }

    @Override
    public void transaction(ConfigSource config, Schema inputSchema,
            FilterPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);

        String json_template = task.getJsonColumn().getTemplate();
        BuildJsonConfigChecker checker = new BuildJsonConfigChecker(json_template,inputSchema);
        checker.validateJSON();

        Schema outputSchema = buildOutputSchema(task, inputSchema);
        for (Column column : outputSchema.getColumns()) {
            logger.debug("OutputSchema: {}", column);
        }

        control.run(task.dump(), outputSchema);
    }

    static Type jsonColumnType(PluginTask task)
    {
        Type columnType = task.getJsonColumn().getType().or(DEFAULT_COLUMN_TYPE);

        if (columnType != Types.JSON && columnType != Types.STRING) {
            throw new SchemaConfigException("columnType must be json or string");
        }
        return columnType;
    }

    static Schema buildOutputSchema(PluginTask task, Schema inputSchema)
    {
        Type jsonColumnType = jsonColumnType(task);
        String jsonColumnName = task.getJsonColumn().getName();

        Schema.Builder builder = Schema.builder();
        int found = 0;

        for (Column inputColumns : inputSchema.getColumns()) {
            if (jsonColumnName.equals(inputColumns.getName())) {
                builder.add(inputColumns.getName(), jsonColumnType);
                found = 1;
            }
            else {
                builder.add(inputColumns.getName(), inputColumns.getType());
            }
        }
        if (found == 0) {
            builder.add(jsonColumnName, jsonColumnType);
        }

        return builder.build();
    }

    @Override
    public PageOutput open(TaskSource taskSource, final Schema inputSchema,
            final Schema outputSchema, final PageOutput output)
    {
        final PluginTask task = taskSource.loadTask(PluginTask.class);

        return new PageOutput()
        {
            private PageReader pageReader = new PageReader(inputSchema);
            private PageBuilder pageBuilder = new PageBuilder(Exec.getBufferAllocator(), outputSchema, output);
            private BuildJsonVisitorImpl visitor = new BuildJsonVisitorImpl(task, inputSchema, outputSchema, pageReader, pageBuilder);

            @Override
            public void finish()
            {
                pageBuilder.finish();
            }

            @Override
            public void close()
            {
                pageBuilder.close();
            }

            @Override
            public void add(Page page)
            {
                pageReader.setPage(page);

                while (pageReader.nextRecord()) {
                    outputSchema.visitColumns(visitor);
                    pageBuilder.addRecord();
                }
            }
        };
    }
}
