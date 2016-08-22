package org.embulk.filter.build_json;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.embulk.spi.Schema;

/**
 * Created by hsato on 2016/08/23.
 */
public class BuildJsonConfigChecker
{
    private String name;
    private String json_template;
    private Schema inputSchema;

    public BuildJsonConfigChecker( String json_template, Schema inputSchema)
    {
        this.json_template = json_template;
        this.inputSchema = inputSchema;
    }

    public Boolean validateJSON()
    {

        ANTLRInputStream input = new ANTLRInputStream(json_template);
        JSONLexer lexer = new JSONLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JSONParser parser = new JSONParser(tokens);

        BuildJsonConfigErrorListener errorListener = new BuildJsonConfigErrorListener();
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        ParseTree tree = parser.json();
        BuildJsonParseTreeConfigVisitor eval = new BuildJsonParseTreeConfigVisitor(inputSchema);
        eval.visit(tree);
        return true;
    }

}
