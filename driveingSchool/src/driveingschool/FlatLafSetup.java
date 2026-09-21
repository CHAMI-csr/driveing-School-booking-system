/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package driveingschool;

/**
 *
 * @author chiki
 */


import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.UIManager;

public class FlatLafSetup {

    public static void setup() {

        try {

            FlatLightLaf.setup();

            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            UIManager.put("TextComponent.arc", 10);

        } catch (Exception e) {

            System.out.println(
                "FlatLaf setup failed: " + e.getMessage()
            );

        }
    }

}
