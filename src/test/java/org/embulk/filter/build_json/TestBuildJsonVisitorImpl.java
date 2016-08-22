package org.embulk.filter.build_json;

import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigLoader;
import org.embulk.config.ConfigSource;
import org.embulk.filter.build_json.BuildJsonFilterPlugin.PluginTask;
import org.embulk.spi.Exec;
import org.embulk.spi.Page;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageReader;
import org.embulk.spi.PageTestUtils;
import org.embulk.spi.Schema;
import org.embulk.spi.TestPageBuilderReader;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.util.Pages;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.msgpack.value.ValueFactory;

import static org.embulk.spi.type.Types.BOOLEAN;
import static org.embulk.spi.type.Types.DOUBLE;
import static org.embulk.spi.type.Types.JSON;
import static org.embulk.spi.type.Types.LONG;
import static org.embulk.spi.type.Types.STRING;
import static org.embulk.spi.type.Types.TIMESTAMP;
import static org.junit.Assert.assertEquals;

import java.util.List;
public class TestBuildJsonVisitorImpl
{
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    @Before
    public void createResource()
    {
    }

    private ConfigSource config()
    {
        return runtime.getExec().newConfigSource();
    }

    private PluginTask taskFromYamlString(String... lines)
    {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line).append("\n");
        }
        String yamlString = builder.toString();

        ConfigLoader loader = new ConfigLoader(Exec.getModelManager());
        ConfigSource config = loader.fromYamlString(yamlString);
        return config.loadConfig(PluginTask.class);
    }

    private List<Object[]> filter(PluginTask task, Schema inputSchema, Object ... objects)
    {
        TestPageBuilderReader.MockPageOutput output = new TestPageBuilderReader.MockPageOutput();
        Schema outputSchema = BuildJsonFilterPlugin.buildOutputSchema(task, inputSchema);
        PageBuilder pageBuilder = new PageBuilder(runtime.getBufferAllocator(), outputSchema, output);
        PageReader pageReader = new PageReader(inputSchema);
        BuildJsonVisitorImpl visitor = new BuildJsonVisitorImpl(task, inputSchema, outputSchema, pageReader, pageBuilder);

        List<Page> pages = PageTestUtils.buildPage(runtime.getBufferAllocator(), inputSchema, objects);
        for (Page page : pages) {
            pageReader.setPage(page);

            while (pageReader.nextRecord()) {
                outputSchema.visitColumns(visitor);
                pageBuilder.addRecord();
            }
        }
        pageBuilder.finish();
        pageBuilder.close();
        return Pages.toObjects(outputSchema, output.pages);
    }

    @Test
    public void visit_build_json_Basic()
    {
        PluginTask task = taskFromYamlString(
                "type: build_json",
                "column: ",
                "  name: json_payload",
                "  type: json",
                "  template: { \"double\":!double }");
        Schema inputSchema = Schema.builder()
                .add("timestamp",TIMESTAMP)
                .add("string",STRING)
                .add("boolean", BOOLEAN)
                .add("long", LONG)
                .add("double",DOUBLE)
                .add("json",JSON)
                .build();
        List<Object[]> records = filter(task, inputSchema,
                // row1
                Timestamp.ofEpochSecond(1436745600), "string", new Boolean(true), new Long(0), new Double(0.5), ValueFactory.newString("json"),
                // row2
                Timestamp.ofEpochSecond(1436745600), "string", new Boolean(true), new Long(0), new Double(0.5), ValueFactory.newString("json"));

        assertEquals(2, records.size());

        Object[] record;
        {
            record = records.get(0);
            assertEquals(7, record.length);
            assertEquals(Timestamp.ofEpochSecond(1436745600),record[0]);
            assertEquals("string",record[1]);
            assertEquals(new Boolean(true),record[2]);
            assertEquals(new Long(0),record[3]);
            assertEquals(new Double(0.5),record[4]);
            assertEquals(ValueFactory.newString("json"),record[5]);
        }
    }
}