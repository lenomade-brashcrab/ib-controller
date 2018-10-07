package ibcontroller;

import java.awt.Component;
import java.awt.Container;
import javax.swing.*;

import static ibcontroller.Utils.selectConfigSection;

class ConfigureApiSettingTask implements Runnable {
    private final boolean readOnlyApi;
    private final boolean bypassOrderPrecautions;
    private final int apiPort;
    private final boolean isGateway;

    ConfigureApiSettingTask(boolean isGateway, int apiPort, boolean readOnlyApi, boolean bypassOrderPrecautions) {
        this.isGateway = isGateway;
        this.apiPort = apiPort;
        this.readOnlyApi = readOnlyApi;
        this.bypassOrderPrecautions = bypassOrderPrecautions;
    }

    @Override
    public void run() {
        try {
            // blocks the thread until the config dialog is available
            final JDialog configDialog = ConfigDialogManager.configDialogManager().getConfigDialog();

            GuiExecutor.instance().execute(new Runnable(){
                @Override
                public void run() {
                    configure(configDialog, apiPort, readOnlyApi, bypassOrderPrecautions);
                }
            });

        } catch (Exception e) {
            Utils.logError("" + e.getMessage());
        }
    }

    private void configure(final JDialog configDialog, final int apiPort, final boolean readOnlyApi, final boolean bypassOrderPrecautions) {
        try {
            Utils.logToConsole("Performing Api setting configuration");

            // older versions of TWS don't have the Settings node below the API node
            selectConfigSection(configDialog, new String[] {"API","Settings"}, new String[] {"API"});

            // set API port
            configureApiPort(configDialog, apiPort);

            // disable/enable ReadOnly API
            setCheckbox(configDialog, "Read-Only API", readOnlyApi);

            // older versions of TWS don't have the Precautions node below the API node
            selectConfigSection(configDialog, new String[]{"API", "Precautions"}, new String[] {"API"});

            // disable/enable Order Precautions
            setCheckbox(configDialog, "Bypass Order Precautions for API Orders", bypassOrderPrecautions);

            // apply settings and close dialog
            SwingUtils.clickButton(configDialog, "OK");

            configDialog.setVisible(false);
        } catch (IBControllerException e) {
            Utils.logError("" + e.getMessage());
        }
    }

    private void configureApiPort(JDialog configDialog, int apiPort) throws IBControllerException {
        if (apiPort != 0) {
            Component comp = SwingUtils.findComponent(configDialog, "Socket port");
            if (comp == null)
                throw new IBControllerException("could not find socket port component");

            JTextField tf = SwingUtils.findTextField((Container)comp, 0);
            if (tf == null) throw new IBControllerException("could not find socket port field");

            int currentPort = Integer.parseInt(tf.getText());
            if (currentPort == apiPort) {
                Utils.logToConsole("TWS API socket port is already set to " + tf.getText());
            } else {
                if (!this.isGateway) {
                    JCheckBox cb = SwingUtils.findCheckBox(configDialog,
                                                      "Enable ActiveX and Socket Clients");
                    if (cb == null) {
                        throw new IBControllerException("could not find Enable ActiveX checkbox");
                    }
                    if (cb.isSelected()) {
                        ConfigDialogManager.configDialogManager().setApiConfigChangeConfirmationExpected(true);
                    }
                }
                Utils.logToConsole("TWS API socket port was set to " + tf.getText());
                tf.setText(Integer.toString(apiPort));
                Utils.logToConsole("TWS API socket port now set to " + tf.getText());
            }
        }
    }

    private void setCheckbox(JDialog configDialog,
                             String checkboxText,
                             boolean checkboxValue) throws IBControllerException {
        JCheckBox cb = findCheckBox(configDialog, checkboxText);
        cb.setSelected(checkboxValue);
        if (checkboxValue) {
            Utils.logToConsole("Select and enable " + checkboxText);
        } else {
            Utils.logToConsole("Unselect and disable " + checkboxText);
        }
    }

    private JCheckBox findCheckBox(JDialog configDialog, String text) throws IBControllerException {
        JCheckBox cb = SwingUtils.findCheckBox(configDialog, text);
        if (cb == null) throw new IBControllerException("could not find " + text + " checkbox");
        return cb;
    }
}
