/*
 * Sonar SSLR :: YAML Parser
 * Copyright (C) 2018-2019 Societe Generale
 * vincent.girard-reydet AT socgen DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.sslr.yaml.snakeyaml.parser;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.Mark;
import com.sonar.sslr.impl.LexerException;
import javax.annotation.Nullable;
import org.sonar.sslr.channel.CodeBuffer;

public class YamlLexerException extends LexerException {
    private String context;
    private CodeBuffer.Cursor contextMark;
    private String problem;
    private CodeBuffer.Cursor problemMark;

    public YamlLexerException(@Nullable String context, @Nullable CodeBuffer.Cursor contextMark, String problem, CodeBuffer.Cursor problemMark) {
        this(context, contextMark, problem, problemMark, null);
    }

    public YamlLexerException(@Nullable String context, @Nullable CodeBuffer.Cursor contextMark, String problem, @Nullable CodeBuffer.Cursor problemMark, Throwable cause) {
        super(context + "; " + problem + "; " + toString(problemMark), cause);
        this.context = context;
        this.contextMark = contextMark == null ? null : contextMark.clone();
        this.problem = problem;
        this.problemMark = problemMark == null ? null : problemMark.clone();
    }

    public String toString() {
        StringBuilder lines = new StringBuilder();
        if (this.context != null) {
            lines.append(this.context);
            lines.append("\n");
        }

        if (this.contextMark != null && (this.problem == null || this.problemMark == null || this.contextMark.getLine() != this.problemMark.getLine() || this.contextMark.getColumn() != this.problemMark.getColumn())) {
            lines.append(toString(contextMark));
            lines.append("\n");
        }

        if (this.problem != null) {
            lines.append(this.problem);
            lines.append("\n");
        }

        if (this.problemMark != null) {
            lines.append(toString(problemMark));
            lines.append("\n");
        }

        return lines.toString();
    }

    public String getContext() {
        return this.context;
    }

    public CodeBuffer.Cursor getContextMark() {
        return this.contextMark;
    }

    public String getProblem() {
        return this.problem;
    }

    public CodeBuffer.Cursor getProblemMark() {
        return this.problemMark;
    }

    private static String toString(@Nullable CodeBuffer.Cursor cursor) {
        if (cursor == null) {
            return "";
        }
        return "line " + cursor.getLine() + ", column " + cursor.getColumn();
    }
}
