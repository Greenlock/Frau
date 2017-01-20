package com.greenlock.frau;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LukeSmalley on 1/19/2017.
 */
public class CommandParser extends StringIterator<List<String>> {

    private List<String> args = new ArrayList<>();
    private String token = "";

    private void pushToken() {
        if (token.length() > 0) {
            args.add(token);
            token = "";
        }
    }


    private Runnable defaultState = new Runnable() {
        @Override
        public void run() {
            if (currentEquals('"')) {
                enter(doubleQuoteState);
            } else if (currentBegins("```")) {
                while (canAdvance()) {
                    advance();
                    if (currentEquals('\n')) {
                        break;
                    }
                }
                enter(multilineCodeState);
            } else if (currentEquals('`')) {
                enter(codeState);
            } else if (currentEquals('\\')) {
                if (canAdvance()) {
                    advance();
                    if (currentEquals('`') || currentEquals('"')) {
                        token += current();
                    } else {
                        token += '\\';
                        token += current();
                    }
                } else {
                    token += current();
                }
            } else if (currentEquals(' ') || currentEquals('\t') || currentEquals('\n')) {
                pushToken();
            } else {
                token += current();
            }
        }
    };

    private Runnable doubleQuoteState = new Runnable() {
        @Override
        public void run() {
            if (currentEquals('"')) {
                leave();
            } else if (currentEquals('\\')) {
                if (canAdvance()) {
                    advance();
                    if (currentEquals('`') || currentEquals('"')) {
                        token += current();
                    } else {
                        token += '\\';
                        token += current();
                    }
                } else {
                    token += current();
                }
            } else if (currentEquals('\n')) {
                pushToken();
                leave();
            } else {
                token += current();
            }
        }
    };

    private Runnable codeState = new Runnable() {
        @Override
        public void run() {
            if (currentEquals('`')) {
                leave();
            } else {
                token += current();
            }
        }
    };

    private Runnable multilineCodeState = new Runnable() {
        @Override
        public void run() {
            if (currentBegins("\n```")) {
                leave();
            } else {
                token += current();
            }
        }
    };


    public CommandParser(String specimen) {
        super(specimen);
        result = args;
        enter(defaultState);
    }

    @Override
    public void onFinish() {
        pushToken();
    }
}
