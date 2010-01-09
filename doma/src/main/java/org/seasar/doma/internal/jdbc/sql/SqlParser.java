/*
 * Copyright 2004-2009 the Seasar Foundation and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.seasar.doma.internal.jdbc.sql;

import static org.seasar.doma.internal.util.AssertionUtil.*;

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.seasar.doma.internal.jdbc.sql.node.AnonymousNode;
import org.seasar.doma.internal.jdbc.sql.node.AnonymousNodeVisitor;
import org.seasar.doma.internal.jdbc.sql.node.BindVariableNode;
import org.seasar.doma.internal.jdbc.sql.node.BlockNode;
import org.seasar.doma.internal.jdbc.sql.node.ElseNode;
import org.seasar.doma.internal.jdbc.sql.node.ElseNodeVisitor;
import org.seasar.doma.internal.jdbc.sql.node.ElseifNode;
import org.seasar.doma.internal.jdbc.sql.node.ElseifNodeVisitor;
import org.seasar.doma.internal.jdbc.sql.node.EmbeddedVariableNode;
import org.seasar.doma.internal.jdbc.sql.node.EndNode;
import org.seasar.doma.internal.jdbc.sql.node.EndNodeVisitor;
import org.seasar.doma.internal.jdbc.sql.node.EolNode;
import org.seasar.doma.internal.jdbc.sql.node.EolNodeVisitor;
import org.seasar.doma.internal.jdbc.sql.node.ForBlockNode;
import org.seasar.doma.internal.jdbc.sql.node.ForNode;
import org.seasar.doma.internal.jdbc.sql.node.ForNodeVisitor;
import org.seasar.doma.internal.jdbc.sql.node.ForUpdateClauseNode;
import org.seasar.doma.internal.jdbc.sql.node.ForUpdateClauseNodeVisitor;
import org.seasar.doma.internal.jdbc.sql.node.FragmentNode;
import org.seasar.doma.internal.jdbc.sql.node.FromClauseNode;
import org.seasar.doma.internal.jdbc.sql.node.FromClauseNodeVisitor;
import org.seasar.doma.internal.jdbc.sql.node.GroupByClauseNode;
import org.seasar.doma.internal.jdbc.sql.node.GroupByClauseNodeVisitor;
import org.seasar.doma.internal.jdbc.sql.node.HavingClauseNode;
import org.seasar.doma.internal.jdbc.sql.node.HavingClauseNodeVisitor;
import org.seasar.doma.internal.jdbc.sql.node.IfBlockNode;
import org.seasar.doma.internal.jdbc.sql.node.IfNode;
import org.seasar.doma.internal.jdbc.sql.node.IfNodeVisitor;
import org.seasar.doma.internal.jdbc.sql.node.LogicalOperatorNode;
import org.seasar.doma.internal.jdbc.sql.node.LogicalOperatorNodeVisitor;
import org.seasar.doma.internal.jdbc.sql.node.OrderByClauseNode;
import org.seasar.doma.internal.jdbc.sql.node.OtherNode;
import org.seasar.doma.internal.jdbc.sql.node.ParensNode;
import org.seasar.doma.internal.jdbc.sql.node.ParensNodeVisitor;
import org.seasar.doma.internal.jdbc.sql.node.SelectClauseNode;
import org.seasar.doma.internal.jdbc.sql.node.SelectClauseNodeVisitor;
import org.seasar.doma.internal.jdbc.sql.node.SelectStatementNode;
import org.seasar.doma.internal.jdbc.sql.node.SelectStatementNodeVisitor;
import org.seasar.doma.internal.jdbc.sql.node.SpaceStrippingNode;
import org.seasar.doma.internal.jdbc.sql.node.SqlLocation;
import org.seasar.doma.internal.jdbc.sql.node.WhereClauseNode;
import org.seasar.doma.internal.jdbc.sql.node.WhereClauseNodeVisitor;
import org.seasar.doma.internal.jdbc.sql.node.WhitespaceNode;
import org.seasar.doma.internal.jdbc.sql.node.WordNode;
import org.seasar.doma.internal.message.Message;
import org.seasar.doma.jdbc.JdbcException;
import org.seasar.doma.jdbc.SqlNode;

/**
 * @author taedium
 * 
 */
public class SqlParser {

    protected final Deque<SqlNode> nodeStack = new LinkedList<SqlNode>();

    protected final String sql;

    protected final SqlTokenizer tokenizer;

    protected final AnonymousNode rootNode;

    protected SqlTokenType tokenType;

    protected String token;

    public SqlParser(String sql) {
        assertNotNull(sql);
        this.sql = sql;
        tokenizer = new SqlTokenizer(sql);
        rootNode = new AnonymousNode();
        nodeStack.push(rootNode);
    }

    public SqlNode parse() {
        outer: for (;;) {
            tokenType = tokenizer.next();
            token = tokenizer.getToken();
            switch (tokenType) {
            case WHITESPACE: {
                parseWhitespace();
                break;
            }
            case WORD:
            case QUOTE: {
                parseWord();
                break;
            }
            case SELECT_WORD: {
                parseSelectWord();
                break;
            }
            case FROM_WORD: {
                parseFromWord();
                break;
            }
            case WHERE_WORD: {
                parseWhereWord();
                break;
            }
            case GROUP_BY_WORD: {
                parseGroupByWord();
                break;
            }
            case HAVING_WORD: {
                parseHavingWord();
                break;
            }
            case ORDER_BY_WORD: {
                parseOrderByWord();
                break;
            }
            case FOR_UPDATE_WORD: {
                parseForUpdateWord();
                break;
            }
            case AND_WORD:
            case OR_WORD: {
                parseLogicalWord();
                break;
            }
            case OPENED_PARENS: {
                parseOpenedParens();
                break;
            }
            case CLOSED_PARENS: {
                parseClosedParens();
                break;
            }
            case BIND_VARIABLE_BLOCK_COMMENT: {
                parseBindVariableBlockComment();
                break;
            }
            case EMBEDDED_VARIABLE_BLOCK_COMMENT: {
                parseEmbeddedVariableBlockComment();
                break;
            }
            case IF_BLOCK_COMMENT: {
                parseIfBlockComment();
                break;
            }
            case ELSEIF_LINE_COMMENT: {
                parseElseifLineComment();
                break;
            }
            case ELSE_LINE_COMMENT: {
                parseElseLineComment();
                break;
            }
            case END_BLOCK_COMMENT: {
                parseEndBlockComment();
                break;
            }
            case FOR_BLOCK_COMMENT: {
                parseForBlockComment();
                break;
            }
            case UNION_WORD:
            case EXCEPT_WORD:
            case MINUS_WORD:
            case INTERSECT_WORD: {
                parseSetOperatorWord();
                break;
            }
            case EOL: {
                parseEOL();
                break;
            }
            case DELIMITER:
            case EOF: {
                validateTermination();
                break outer;
            }
            default: {
                parseOther();
                break;
            }
            }
        }
        new Optimizer().optimize(rootNode);
        return rootNode;
    }

    protected void parseSetOperatorWord() {
        AnonymousNode node = new AnonymousNode();
        node.addNode(new WordNode(token));
        if (isInSelectStatementNode()) {
            removeNodesTo(SelectStatementNode.class);
            pop();
        }
        addNode(node);
        push(node);
    }

    protected void parseSelectWord() {
        SelectStatementNode selectStatementNode = new SelectStatementNode();
        addNode(selectStatementNode);
        push(selectStatementNode);
        SelectClauseNode selectClauseNode = new SelectClauseNode(token);
        selectStatementNode.setSelectClauseNode(selectClauseNode);
        push(selectClauseNode);
    }

    protected void parseFromWord() {
        FromClauseNode node = new FromClauseNode(token);
        if (isInSelectStatementNode()) {
            removeNodesTo(SelectStatementNode.class);
            SelectStatementNode selectStatementNode = peek();
            selectStatementNode.setFromClauseNode(node);
        } else {
            addNode(node);
        }
        push(node);
    }

    protected void parseWhereWord() {
        WhereClauseNode node = new WhereClauseNode(token);
        if (isInSelectStatementNode()) {
            removeNodesTo(SelectStatementNode.class);
            SelectStatementNode selectStatementNode = peek();
            selectStatementNode.setWhereClauseNode(node);
        } else {
            addNode(node);
        }
        push(node);
    }

    protected void parseGroupByWord() {
        GroupByClauseNode node = new GroupByClauseNode(token);
        if (isInSelectStatementNode()) {
            removeNodesTo(SelectStatementNode.class);
            SelectStatementNode selectStatementNode = peek();
            selectStatementNode.setGroupByClauseNode(node);
        } else {
            addNode(node);
        }
        push(node);
    }

    protected void parseHavingWord() {
        HavingClauseNode node = new HavingClauseNode(token);
        if (isInSelectStatementNode()) {
            removeNodesTo(SelectStatementNode.class);
            SelectStatementNode selectStatementNode = peek();
            selectStatementNode.setHavingClauseNode(node);
        } else {
            addNode(node);
        }
        push(node);
    }

    protected void parseOrderByWord() {
        OrderByClauseNode node = new OrderByClauseNode(token);
        if (isInSelectStatementNode()) {
            removeNodesTo(SelectStatementNode.class);
            SelectStatementNode selectStatementNode = peek();
            selectStatementNode.setOrderByClauseNode(node);
        } else {
            addNode(node);
        }
        push(node);
    }

    protected void parseForUpdateWord() {
        ForUpdateClauseNode node = new ForUpdateClauseNode(token);
        if (isInSelectStatementNode()) {
            removeNodesTo(SelectStatementNode.class);
            SelectStatementNode selectStatementNode = peek();
            selectStatementNode.setForUpdateClauseNode(node);
        } else {
            addNode(node);
        }
        push(node);
    }

    protected void parseLogicalWord() {
        String word = tokenType.extract(token);
        LogicalOperatorNode node = new LogicalOperatorNode(word);
        addNode(node);
        push(node);
    }

    protected void parseWord() {
        WordNode node = new WordNode(token);
        addNode(node);
    }

    protected void parseOpenedParens() {
        ParensNode parensNode = new ParensNode();
        addNode(parensNode);
        push(parensNode);
    }

    protected void parseClosedParens() {
        if (!isInParensNode()) {
            throw new JdbcException(Message.DOMA2109, sql, tokenizer
                    .getLineNumber(), tokenizer.getPosition());
        }
        removeNodesTo(ParensNode.class);
        ParensNode parensNode = pop();
        parensNode.close();
    }

    protected void parseBindVariableBlockComment() {
        String varialbeName = tokenType.extract(token);
        if (varialbeName.isEmpty()) {
            throw new JdbcException(Message.DOMA2120, sql, tokenizer
                    .getLineNumber(), tokenizer.getPosition(), token);
        }
        BindVariableNode node = new BindVariableNode(getLocation(),
                varialbeName, token);
        addNode(node);
        push(node);
    }

    protected void parseEmbeddedVariableBlockComment() {
        String varialbeName = tokenType.extract(token);
        if (varialbeName.isEmpty()) {
            throw new JdbcException(Message.DOMA2121, sql, tokenizer
                    .getLineNumber(), tokenizer.getPosition(), token);
        }
        EmbeddedVariableNode node = new EmbeddedVariableNode(getLocation(),
                varialbeName, token);
        addNode(node);
        push(node);
    }

    protected void parseIfBlockComment() {
        removePrecedentSpaces();
        IfBlockNode ifBlockNode = new IfBlockNode();
        addNode(ifBlockNode);
        push(ifBlockNode);
        String expression = tokenType.extract(token);
        IfNode ifNode = new IfNode(getLocation(), expression, token);
        ifBlockNode.setIfNode(ifNode);
        push(ifNode);
    }

    protected void parseElseifLineComment() {
        if (!isInIfBlockNode()) {
            throw new JdbcException(Message.DOMA2106, sql, tokenizer
                    .getLineNumber(), tokenizer.getPosition());
        }
        removeNodesTo(IfBlockNode.class);
        IfBlockNode ifBlockNode = peek();
        if (ifBlockNode.isElseNodeExistent()) {
            throw new JdbcException(Message.DOMA2108, sql, tokenizer
                    .getLineNumber(), tokenizer.getPosition());
        }
        String expression = tokenType.extract(token);
        ElseifNode node = new ElseifNode(getLocation(), expression, token);
        ifBlockNode.addElseifNode(node);
        push(node);
    }

    protected void parseElseLineComment() {
        if (!isInIfBlockNode()) {
            throw new JdbcException(Message.DOMA2105, sql, tokenizer
                    .getLineNumber(), tokenizer.getPosition());
        }
        removeNodesTo(IfBlockNode.class);
        IfBlockNode ifBlockNode = peek();
        if (ifBlockNode.isElseNodeExistent()) {
            throw new JdbcException(Message.DOMA2107, sql, tokenizer
                    .getLineNumber(), tokenizer.getPosition());
        }
        ElseNode node = new ElseNode(token);
        ifBlockNode.setElseNode(node);
        push(node);
    }

    protected void parseEndBlockComment() {
        if (!isInBlockNode()) {
            throw new JdbcException(Message.DOMA2104, sql, tokenizer
                    .getLineNumber(), tokenizer.getPosition());
        }
        removePrecedentSpaces();
        removeNodesTo(BlockNode.class);
        BlockNode blockNode = pop();
        EndNode node = new EndNode(token);
        blockNode.setEndNode(node);
        push(node);
    }

    protected void parseForBlockComment() {
        removePrecedentSpaces();
        ForBlockNode forBlockNode = new ForBlockNode();
        addNode(forBlockNode);
        push(forBlockNode);
        String expr = tokenType.extract(token);
        int pos = expr.indexOf(":");
        if (pos == -1) {
            throw new JdbcException(Message.DOMA2124, sql, tokenizer
                    .getLineNumber(), tokenizer.getPosition());
        }
        String identifier = expr.substring(0, pos).trim();
        if (identifier.isEmpty()) {
            throw new JdbcException(Message.DOMA2125, sql, tokenizer
                    .getLineNumber(), tokenizer.getPosition());
        }
        String expression = expr.substring(pos + 1).trim();
        if (expression.isEmpty()) {
            throw new JdbcException(Message.DOMA2126, sql, tokenizer
                    .getLineNumber(), tokenizer.getPosition());
        }
        ForNode forNode = new ForNode(getLocation(), identifier, expression,
                token);
        forBlockNode.setForNode(forNode);
        push(forNode);
    }

    protected void parseOther() {
        addNode(OtherNode.of(token));
    }

    protected void parseEOL() {
        if (isAfterSpaceStrippingNode()) {
            SpaceStrippingNode spaceStrippingNode = peek();
            if (containsOnlyWhitespaces(spaceStrippingNode)) {
                spaceStrippingNode.clearChildren();
                return;
            }
        }
        EolNode node = new EolNode(token);
        addNode(node);
        push(node);
    }

    protected boolean containsOnlyWhitespaces(SqlNode node) {
        for (SqlNode child : node.getChildren()) {
            if (!(child instanceof WhitespaceNode)) {
                return false;
            }
        }
        return true;
    }

    protected void parseWhitespace() {
        addNode(WhitespaceNode.of(token));
    }

    protected void removeNodesTo(Class<? extends SqlNode> clazz) {
        for (Iterator<SqlNode> it = nodeStack.iterator(); it.hasNext();) {
            SqlNode node = it.next();
            if (clazz.isInstance(node)) {
                break;
            }
            it.remove();
        }
    }

    protected void removePrecedentSpaces() {
        if (isAfterEolNode()) {
            EolNode eolNode = peek();
            if (containsOnlyWhitespaces(eolNode)) {
                eolNode.clearChildren();
            }
        } else if (isAfterSpaceStrippingNode()) {
            SpaceStrippingNode spaceStrippingNode = peek();
            if (containsOnlyWhitespaces(spaceStrippingNode)) {
                spaceStrippingNode.clearChildren();
            }
        }
    }

    protected boolean isInSelectStatementNode() {
        for (SqlNode node : nodeStack) {
            if (node instanceof ParensNode) {
                return false;
            }
            if (node instanceof SelectStatementNode) {
                return true;
            }
        }
        return false;
    }

    protected boolean isInIfBlockNode() {
        for (SqlNode node : nodeStack) {
            if (node instanceof IfBlockNode) {
                return true;
            }
        }
        return false;
    }

    protected boolean isInForBlockNode() {
        for (SqlNode node : nodeStack) {
            if (node instanceof ForBlockNode) {
                return true;
            }
        }
        return false;
    }

    protected boolean isInParensNode() {
        for (SqlNode node : nodeStack) {
            if (node instanceof ParensNode) {
                return true;
            }
        }
        return false;
    }

    protected boolean isInBlockNode() {
        for (SqlNode node : nodeStack) {
            if (node instanceof BlockNode) {
                return true;
            }
        }
        return false;
    }

    protected boolean isAfterEolNode() {
        return peek() instanceof EolNode;
    }

    protected boolean isAfterSpaceStrippingNode() {
        return peek() instanceof SpaceStrippingNode;
    }

    protected boolean isAfterBindVariableNode() {
        return peek() instanceof BindVariableNode;
    }

    protected void addNode(SqlNode node) {
        if (isAfterBindVariableNode()) {
            BindVariableNode bindVariableNode = pop();
            if (node instanceof WordNode) {
                WordNode wordNode = (WordNode) node;
                bindVariableNode.setWordNode(wordNode);
            } else if (node instanceof ParensNode) {
                ParensNode parensNode = (ParensNode) node;
                parensNode.setAttachedWithBindVariable(true);
                bindVariableNode.setParensNode(parensNode);
            } else {
                throw new JdbcException(Message.DOMA2110, sql, tokenizer
                        .getLineNumber(), tokenizer.getPosition(),
                        bindVariableNode.getText());
            }
        } else {
            peek().addNode(node);
        }
    }

    protected void validateTermination() {
        if (isAfterBindVariableNode()) {
            BindVariableNode bindVariableNode = pop();
            throw new JdbcException(Message.DOMA2110, sql, tokenizer
                    .getLineNumber(), tokenizer.getPosition(), bindVariableNode
                    .getText());
        }
    }

    protected void push(SqlNode node) {
        nodeStack.push(node);
    }

    @SuppressWarnings("unchecked")
    protected <T extends SqlNode> T peek() {
        return (T) nodeStack.peek();
    }

    @SuppressWarnings("unchecked")
    protected <T extends SqlNode> T pop() {
        return (T) nodeStack.pop();
    }

    protected SqlLocation getLocation() {
        return new SqlLocation(sql, tokenizer.getLineNumber(), tokenizer
                .getPosition());
    }

    protected static class Optimizer implements
            SelectStatementNodeVisitor<Void, Void>,
            SelectClauseNodeVisitor<Void, Void>,
            FromClauseNodeVisitor<Void, Void>,
            WhereClauseNodeVisitor<Void, Void>,
            HavingClauseNodeVisitor<Void, Void>,
            GroupByClauseNodeVisitor<Void, Void>,
            ForUpdateClauseNodeVisitor<Void, Void>, IfNodeVisitor<Void, Void>,
            ElseifNodeVisitor<Void, Void>, ElseNodeVisitor<Void, Void>,
            LogicalOperatorNodeVisitor<Void, Void>, ForNodeVisitor<Void, Void>,
            EndNodeVisitor<Void, Void>, AnonymousNodeVisitor<Void, Void>,
            ParensNodeVisitor<Void, Void>, EolNodeVisitor<Void, Void> {

        public void optimize(SqlNode node) {
            node.accept(this, null);
        }

        protected void optimizeChildren(SqlNode node) {
            List<SqlNode> children = new ArrayList<SqlNode>(node.getChildren());
            node.getChildren().clear();

            StringBuilder buf = new StringBuilder();
            for (SqlNode child : children) {
                if (child instanceof WordNode) {
                    buf.append(((WordNode) child).getWord());
                } else if (child instanceof OtherNode) {
                    buf.append(((OtherNode) child).getOther());
                } else if (buf.length() > 0 && child instanceof WhitespaceNode) {
                    buf.append(((WhitespaceNode) child).getWhitespace());
                } else {
                    if (buf.length() > 0) {
                        node.addNode(new FragmentNode(buf.toString()));
                        buf.setLength(0);
                    }
                    if (child.getChildren().size() > 0) {
                        child.accept(this, null);
                    }
                    node.addNode(child);
                }
            }
            if (buf.length() > 0) {
                node.addNode(new FragmentNode(buf.toString()));
                buf.setLength(0);
            }
        }

        @Override
        public Void visitSelectStatementNode(SelectStatementNode node, Void p) {
            for (SqlNode child : node.getChildren()) {
                child.accept(this, p);
            }
            return null;
        }

        @Override
        public Void visitSelectClauseNode(SelectClauseNode node, Void p) {
            optimizeChildren(node);
            return null;
        }

        @Override
        public Void visitAnonymousNode(AnonymousNode node, Void p) {
            optimizeChildren(node);
            return null;
        }

        @Override
        public Void visitParensNode(ParensNode node, Void p) {
            optimizeChildren(node);
            return null;
        }

        @Override
        public Void visitIfNode(IfNode node, Void p) {
            optimizeChildren(node);
            return null;
        }

        @Override
        public Void visitEndNode(EndNode node, Void p) {
            optimizeChildren(node);
            return null;
        }

        @Override
        public Void visitFromClauseNode(FromClauseNode node, Void p) {
            optimizeChildren(node);
            return null;
        }

        @Override
        public Void visitForNode(ForNode node, Void p) {
            optimizeChildren(node);
            return null;
        }

        @Override
        public Void visitHavingClauseNode(HavingClauseNode node, Void p) {
            optimizeChildren(node);
            return null;
        }

        @Override
        public Void visitWhereClauseNode(WhereClauseNode node, Void p) {
            optimizeChildren(node);
            return null;
        }

        @Override
        public Void visitLogicalOperatorNode(LogicalOperatorNode node, Void p) {
            optimizeChildren(node);
            return null;
        }

        @Override
        public Void visitEolNode(EolNode node, Void p) {
            optimizeChildren(node);
            return null;
        }

        @Override
        public Void visitElseifNode(ElseifNode node, Void p) {
            optimizeChildren(node);
            return null;
        }

        @Override
        public Void visitForUpdateClauseNode(ForUpdateClauseNode node, Void p) {
            optimizeChildren(node);
            return null;
        }

        @Override
        public Void visitGroupByClauseNode(GroupByClauseNode node, Void p) {
            optimizeChildren(node);
            return null;
        }

        @Override
        public Void visitElseNode(ElseNode node, Void p) {
            optimizeChildren(node);
            return null;
        }

        @Override
        public Void visitUnknownNode(SqlNode node, Void p) {
            return null;
        }
    }

}
