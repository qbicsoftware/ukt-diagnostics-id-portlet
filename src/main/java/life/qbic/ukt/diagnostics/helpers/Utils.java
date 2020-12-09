/*******************************************************************************
 * QBiC Project qNavigator enables users to manage their projects. Copyright (C) "2016”
 * Christopher Mohr, David Wojnar, Andreas Friedrich
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package life.qbic.ukt.diagnostics.helpers;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Page;
import com.vaadin.server.StreamResource;
import com.vaadin.shared.Position;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.themes.ValoTheme;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Utils {

    /**
     * Checks if a String can be parsed to an Integer
     *
     * @param s a String
     * @return true, if the String can be parsed to an Integer successfully, false otherwise
     */
    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    /**
     * Parses a whole String list to integers and returns them in another list.
     *
     * @param strings List of Strings
     * @return list of integer representations of the input list
     */
    public static List<Integer> strArrToInt(List<String> strings) {
        List<Integer> res = new ArrayList<>();
        for (String s : strings) {
            res.add(Integer.parseInt(s));
        }
        return res;
    }

    /**
     * Maps an integer to a char representation. This can be used for computing the checksum.
     *
     * @param i number to be mapped
     * @return char representing the input number
     */
    public static char mapToChar(int i) {
        i += 48;
        if (i > 57) {
            i += 7;
        }
        return (char) i;
    }

    /**
     * Checks which of two Strings can be parsed to a larger Integer and returns it.
     *
     * @param a a String
     * @param b another String
     * @return the String that represents the larger number.
     */
    public static String max(String a, String b) {
        int a1 = Integer.parseInt(a);
        int b1 = Integer.parseInt(b);
        if (Math.max(a1, b1) == a1)
            return a;
        else
            return b;
    }

    /**
     * Creates a string with leading zeroes from a number
     *
     * @param id number
     * @param length of the final string
     * @return the completed String with leading zeroes
     */
    public static String createCountString(int id, int length) {
        String res = Integer.toString(id);
        while (res.length() < length) {
            res = "0" + res;
        }
        return res;
    }

    /**
     * Increments the value of an upper case char. When at "X" restarts with "A".
     *
     * @param c the char to be incremented
     * @return the next letter in the alphabet relative to the input char
     */
    public static char incrementUppercase(char c) {
        if (c == 'X')
            return 'A';
        else {
            int charValue = c;
            return (char) (charValue + 1);
        }
    }

    public static StreamResource getTSVStream(final String content, String id) {
        StreamResource resource = new StreamResource(new StreamResource.StreamSource() {
            private static final long serialVersionUID = 946357391804404061L;

            @Override
            public InputStream getStream() {
                try {
                    InputStream is = new ByteArrayInputStream(content.getBytes());
                    return is;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            // remove slashes and get rid of leading underscore afterwards
        }, String.format("%s_table_contents.tsv", id.replace("/", "_").substring(1)));
        return resource;
    }



    public static String getTime() {
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZZ");
        return ft.format(dNow);
    }


    public static void Notification(String title, String description, String type) {
        Notification notify = new Notification(title, description);
        notify.setPosition(Position.TOP_CENTER);
        if (type.equals("error")) {
            notify.setDelayMsec(3000);
            notify.setIcon(VaadinIcons.FROWN_O);
            notify.setStyleName(ValoTheme.NOTIFICATION_ERROR + " " + ValoTheme.NOTIFICATION_CLOSABLE);
        } else if (type.equals("success")) {
            notify.setDelayMsec(3000);
            notify.setIcon(VaadinIcons.SMILEY_O);
            notify.setStyleName(ValoTheme.NOTIFICATION_SUCCESS + " " + ValoTheme.NOTIFICATION_CLOSABLE);
        } else {
            notify.setDelayMsec(3000);
            notify.setIcon(VaadinIcons.COMMENT);
            notify.setStyleName(ValoTheme.NOTIFICATION_TRAY + " " + ValoTheme.NOTIFICATION_CLOSABLE);
        }
        notify.show(Page.getCurrent());
    }

    public static Panel createInfoBox(String caption, String description) {
        Panel panel = new Panel(caption);
        panel.setIcon(VaadinIcons.INFO);
        panel.setStyleName(ValoTheme.PANEL_BORDERLESS);
        HorizontalLayout layout = new HorizontalLayout();
        Label label = new Label();
        label.setValue(description);
        layout.addComponent(label);

        panel.setContent(layout);
        return panel;
    }

}