package org.bioviz.protannot;

import javax.swing.JOptionPane;

final public class Reporter {

    /**
     * Report message to the user, optionally showing a JOptionPane
     * widget, printing to stderr or stdout, as requested by the caller.
     *
     * @param message
     * @param ex
     * @param print_to_stderr
     * @param print_stack_trace
     * @param show_dialog
     */
    public static void report(String message,
            Exception ex,
            boolean print_to_stderr,
            boolean print_stack_trace,
            boolean show_dialog) {
        if (print_to_stderr) {
            System.err.println(message);
        }
        if (print_stack_trace && ex != null) {
            ex.printStackTrace();
        }
        if (show_dialog) {
            JOptionPane.showMessageDialog(null, message, "ProtAnnot Alert",
                JOptionPane.ERROR_MESSAGE);
        }
    }
}
