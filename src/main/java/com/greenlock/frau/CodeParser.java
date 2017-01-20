package com.greenlock.frau;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LukeSmalley on 1/7/2017.
 */
public class CodeParser extends StringIterator<String> {

    private String code = "";


    private Runnable defaultState = new Runnable() {
        @Override
        public void run() {
            if (currentBegins("```")) {
                while (canAdvance()) {
                    advance();
                    if (currentEquals('\n')) {
                        break;
                    }
                }
                enter(multilineCodeState);
            } else if (currentEquals('`')) {
                enter(codeState);
            }
        }
    };

    private Runnable codeState = new Runnable() {
        @Override
        public void run() {
            if (currentEquals('`')) {
                leave();
            } else {
                code += current();
            }
        }
    };

    private Runnable multilineCodeState = new Runnable() {
        @Override
        public void run() {
            if (currentBegins("\n```")) {
                leave();
            } else {
                code += current();
            }
        }
    };


    public CodeParser(String specimen) {
        super(specimen);
        enter(defaultState);
    }

    @Override
    public void onFinish() {
        result = code;
    }
}
