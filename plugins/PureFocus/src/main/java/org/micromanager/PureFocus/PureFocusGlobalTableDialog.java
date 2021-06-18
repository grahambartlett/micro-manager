/**
@file PureFocusGlobalTableDialog.java
@author Graham Bartlett
@copyright Prior Scientific Instruments Limited, 2021
@brief Micro-Manager plugin for control of the Prior PureFocus PF-850 autofocus unit

Micro-Manager plugin giving GUI control of the PF-850 setup and configuration.

Licensed under the BSD license.
*/

package org.micromanager.PureFocus;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerModel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mmcorej.CMMCore;
import mmcorej.DeviceType;
import net.miginfocom.swing.MigLayout;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import org.micromanager.Studio;
import org.micromanager.internal.utils.WindowPositioning;


/** Child dialog for control of global settings.  This is mostly intended to
 * get us started before completing the GUI, and for advanced users later.
 */
@SuppressWarnings(value = {"serial", "static-access"})  
public class PureFocusGlobalTableDialog extends JDialog implements ActionListener
{
    /** Reference to the MM/ImageJ GUI */
	private final Studio gui_;
    
    /** Reference to the plugin which owns this */
	private final PureFocus plugin_;
    
    /** Reference to the frame which owns this */
	private final PureFocusFrame parent_;
	
	// GUI elements
    private JCheckBox isPiezoMotor_;
    private JCheckBox servoOn_;
    private JCheckBox servoInhibit_;
    private JCheckBox focusInterruptOn_;
    private JCheckBox interfaceInhibit_;
    private JTextField interfaceInhibitCount_;
    private JCheckBox digipotControlsOffset_;
    private JCheckBox isServoDirectionPositive_;
    private JCheckBox isFocusDriveDirectionPositive_;
    private JTextField exposureTimeUs_;
    private JTextField digipotOffsetSpeedPercent_;
    private JTextField focusDriveRangeMicrons_;
    private JTextField inFocusRecoveryTimeMs_;
    
    /** Set true when reading values back, to block change events */
    boolean updateInProgress_;
    

	/** Create dialog.  This is always instantiated when the main window is
     * created, and is shown or hidden as required.
	 * @param parent Base window
     * @param plugin PureFocus plugin
	 * @param gui MM script interface
   	 */
	public PureFocusGlobalTableDialog(PureFocusFrame parent, PureFocus plugin, Studio gui)
	{
        // We want this to be a modeless dialog, to work with the rest of the GUI
        super(parent, "Prior PureFocus PF-850 Global Settings");
            
		parent_ = parent;
        plugin_ = plugin;
        gui_ = gui;
        
		initComponents();
        
        super.setIconImage(Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource("/org/micromanager/icons/microscope.gif")));
        
        updateInProgress_ = false;
		updateValues();
	}
    

	/** This method is called from within the constructor to initialize the form.
	*/
	@SuppressWarnings("unchecked")
	private void initComponents()
	{
        int i;

        this.setLayout(new MigLayout("", "", ""));
        
        isPiezoMotor_ = new javax.swing.JCheckBox();
        isPiezoMotor_.addActionListener(this);
        isPiezoMotor_.setActionCommand(plugin_.IS_PIEZO_MOTOR);
        
        servoOn_ = new javax.swing.JCheckBox();
        servoOn_.addActionListener(this);   
        servoOn_.setActionCommand(plugin_.SERVO_ON);

        servoInhibit_ = new javax.swing.JCheckBox();
        servoInhibit_.addActionListener(this); 
        servoInhibit_.setActionCommand(plugin_.SERVO_INHIBIT);

        focusInterruptOn_ = new javax.swing.JCheckBox();
        focusInterruptOn_.addActionListener(this);   
        focusInterruptOn_.setActionCommand(plugin_.FOCUS_INTERRUPT_ON);

        interfaceInhibit_ = new javax.swing.JCheckBox();
        interfaceInhibit_.addActionListener(this);   
        interfaceInhibit_.setActionCommand(plugin_.INTERFACE_INHIBIT);
        
        interfaceInhibitCount_ = new javax.swing.JTextField();
        interfaceInhibitCount_.setPreferredSize(new java.awt.Dimension(100, 20));
        interfaceInhibitCount_.addActionListener(this);   
        interfaceInhibitCount_.setActionCommand(plugin_.INTERFACE_INHIBIT_COUNT);

        digipotControlsOffset_ = new javax.swing.JCheckBox();
        digipotControlsOffset_.addActionListener(this); 
        digipotControlsOffset_.setActionCommand(plugin_.DIGIPOT_CONTROLS_OFFSET);

        isServoDirectionPositive_ = new javax.swing.JCheckBox();
        isServoDirectionPositive_.addActionListener(this);  
        isServoDirectionPositive_.setActionCommand(plugin_.IS_SERVO_DIRECTION_POSITIVE);

        isFocusDriveDirectionPositive_ = new javax.swing.JCheckBox();
        isFocusDriveDirectionPositive_.addActionListener(this);
        isFocusDriveDirectionPositive_.setActionCommand(plugin_.IS_FOCUS_DRIVE_DIRECTION_POSITIVE);
        
        exposureTimeUs_ = new javax.swing.JTextField();
        exposureTimeUs_.setPreferredSize(new java.awt.Dimension(100, 20));
        exposureTimeUs_.addActionListener(this); 
        exposureTimeUs_.setActionCommand(plugin_.EXPOSURE_TIME_US);
        
        digipotOffsetSpeedPercent_ = new javax.swing.JTextField();
        digipotOffsetSpeedPercent_.setPreferredSize(new java.awt.Dimension(100, 20));
        digipotOffsetSpeedPercent_.addActionListener(this); 
        digipotOffsetSpeedPercent_.setActionCommand(plugin_.DIGIPOT_OFFSET_SPEED_PERCENT);
        
        focusDriveRangeMicrons_ = new javax.swing.JTextField();
        focusDriveRangeMicrons_.setPreferredSize(new java.awt.Dimension(100, 20));
        focusDriveRangeMicrons_.addActionListener(this);
        focusDriveRangeMicrons_.setActionCommand(plugin_.FOCUS_DRIVE_RANGE_MICRONS);
        
        inFocusRecoveryTimeMs_ = new javax.swing.JTextField();
        inFocusRecoveryTimeMs_.setPreferredSize(new java.awt.Dimension(100, 20));
        inFocusRecoveryTimeMs_.addActionListener(this); 
        inFocusRecoveryTimeMs_.setActionCommand(plugin_.IN_FOCUS_RECOVERY_TIME_MS);
        
        // Add to layout
        this.add(new JLabel("Is piezo motor"), "align label");
        this.add(isPiezoMotor_, "wrap");
        
        this.add(new JLabel("Servo on"), "align label");
        this.add(servoOn_, "wrap");
        
        this.add(new JLabel("Servo inhibit"), "align label");
        this.add(servoInhibit_, "wrap");
        
        this.add(new JLabel("Focus interrupt on"), "align label");
        this.add(focusInterruptOn_, "wrap");
        
        this.add(new JLabel("Interface inhibit"), "align label");
        this.add(interfaceInhibit_, "wrap");
        
        this.add(new JLabel("Interface inhibit count"), "align label");
        this.add(interfaceInhibitCount_, "wrap");
        
        this.add(new JLabel("Digipot controls offset"), "align label");
        this.add(digipotControlsOffset_, "wrap");
        
        this.add(new JLabel("Is servo direction positive"), "align label");
        this.add(isServoDirectionPositive_, "wrap");
        
        this.add(new JLabel("Is focus drive direction positive"), "align label");
        this.add(isFocusDriveDirectionPositive_, "wrap");
        
        this.add(new JLabel("Exposure time (us)"), "align label");
        this.add(exposureTimeUs_, "wrap");
        
        this.add(new JLabel("Digipot offset speed (%)"), "align label");
        this.add(digipotOffsetSpeedPercent_, "wrap");
        
        this.add(new JLabel("Focus drive range (um)"), "align label");
        this.add(focusDriveRangeMicrons_, "wrap");
        
        this.add(new JLabel("In focus recovery time (ms)"), "align label");
        this.add(inFocusRecoveryTimeMs_, "wrap");

        pack();
	}
    
    
    public void updateValues()
	{
        String pf = parent_.getPureFocus();
        CMMCore core = gui_.getCMMCore();
        
        updateInProgress_ = true;

        try
		{
            String value;
            
            value = core.getProperty(pf, plugin_.IS_PIEZO_MOTOR);
            core.defineConfig(plugin_.CONFIG_GROUP, plugin_.CONFIG_GROUP_PRESET, plugin_.DEVICE_NAME, plugin_.IS_PIEZO_MOTOR, value);
            isPiezoMotor_.setSelected(Long.valueOf(value) != 0);
            
            value = core.getProperty(pf, plugin_.SERVO_ON);
            core.defineConfig(plugin_.CONFIG_GROUP, plugin_.CONFIG_GROUP_PRESET, plugin_.DEVICE_NAME, plugin_.SERVO_ON, value);
            servoOn_.setSelected(Long.valueOf(value) != 0);
            
            value = core.getProperty(pf, plugin_.SERVO_INHIBIT);
            core.defineConfig(plugin_.CONFIG_GROUP, plugin_.CONFIG_GROUP_PRESET, plugin_.DEVICE_NAME, plugin_.SERVO_INHIBIT, value);
            servoInhibit_.setSelected(Long.valueOf(value) != 0);
            
            value = core.getProperty(pf, plugin_.FOCUS_INTERRUPT_ON);
            core.defineConfig(plugin_.CONFIG_GROUP, plugin_.CONFIG_GROUP_PRESET, plugin_.DEVICE_NAME, plugin_.FOCUS_INTERRUPT_ON, value);
            focusInterruptOn_.setSelected(Long.valueOf(value) != 0);
            
            value = core.getProperty(pf, plugin_.INTERFACE_INHIBIT);
            core.defineConfig(plugin_.CONFIG_GROUP, plugin_.CONFIG_GROUP_PRESET, plugin_.DEVICE_NAME, plugin_.INTERFACE_INHIBIT, value);
            interfaceInhibit_.setSelected(Long.valueOf(value) != 0);
            
            value = core.getProperty(pf, plugin_.INTERFACE_INHIBIT_COUNT);
            core.defineConfig(plugin_.CONFIG_GROUP, plugin_.CONFIG_GROUP_PRESET, plugin_.DEVICE_NAME, plugin_.INTERFACE_INHIBIT_COUNT, value);
            interfaceInhibitCount_.setText(value);
            
            value = core.getProperty(pf, plugin_.DIGIPOT_CONTROLS_OFFSET);
            core.defineConfig(plugin_.CONFIG_GROUP, plugin_.CONFIG_GROUP_PRESET, plugin_.DEVICE_NAME, plugin_.DIGIPOT_CONTROLS_OFFSET, value);
            digipotControlsOffset_.setSelected(Long.valueOf(value) != 0);
            
            value = core.getProperty(pf, plugin_.IS_SERVO_DIRECTION_POSITIVE);
            core.defineConfig(plugin_.CONFIG_GROUP, plugin_.CONFIG_GROUP_PRESET, plugin_.DEVICE_NAME, plugin_.IS_SERVO_DIRECTION_POSITIVE, value);
            isServoDirectionPositive_.setSelected(Long.valueOf(value) != 0);
            
            value = core.getProperty(pf, plugin_.IS_FOCUS_DRIVE_DIRECTION_POSITIVE);
            core.defineConfig(plugin_.CONFIG_GROUP, plugin_.CONFIG_GROUP_PRESET, plugin_.DEVICE_NAME, plugin_.IS_FOCUS_DRIVE_DIRECTION_POSITIVE, value);
            isFocusDriveDirectionPositive_.setSelected(Long.valueOf(value) != 0);
            
            value = core.getProperty(pf, plugin_.EXPOSURE_TIME_US);
            core.defineConfig(plugin_.CONFIG_GROUP, plugin_.CONFIG_GROUP_PRESET, plugin_.DEVICE_NAME, plugin_.EXPOSURE_TIME_US, value);
            exposureTimeUs_.setText(value);
            
            value = core.getProperty(pf, plugin_.DIGIPOT_OFFSET_SPEED_PERCENT);
            core.defineConfig(plugin_.CONFIG_GROUP, plugin_.CONFIG_GROUP_PRESET, plugin_.DEVICE_NAME, plugin_.DIGIPOT_OFFSET_SPEED_PERCENT, value);
            digipotOffsetSpeedPercent_.setText(value);
            
            value = core.getProperty(pf, plugin_.FOCUS_DRIVE_RANGE_MICRONS);
            core.defineConfig(plugin_.CONFIG_GROUP, plugin_.CONFIG_GROUP_PRESET, plugin_.DEVICE_NAME, plugin_.FOCUS_DRIVE_RANGE_MICRONS, value);
            focusDriveRangeMicrons_.setText(value);
            
            value = core.getProperty(pf, plugin_.IN_FOCUS_RECOVERY_TIME_MS);
            core.defineConfig(plugin_.CONFIG_GROUP, plugin_.CONFIG_GROUP_PRESET, plugin_.DEVICE_NAME, plugin_.IN_FOCUS_RECOVERY_TIME_MS, value);
            inFocusRecoveryTimeMs_.setText(value);
		}
		catch (Exception ex)
		{
			gui_.logs().showError(ex.getMessage());
		}
        
        updateInProgress_ = false;
	}

				
    @Override
    public void actionPerformed(ActionEvent e)
	{
        String pf = parent_.getPureFocus();
        CMMCore core = gui_.getCMMCore();            
        Object source = e.getSource();
        String propertyName = e.getActionCommand();
        
        if (!updateInProgress_)
        {
            try
            {
                core.setProperty(plugin_.DEVICE_NAME, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);

                if (source.getClass() == JTextField.class)
                {
                    JTextField widget = (JTextField)source;
                    String val = widget.getText();
                    Double value = Double.valueOf(val);
                    core.setProperty(pf, propertyName, value);                
                }
                else if (source.getClass() == JCheckBox.class)
                {
                    JCheckBox widget = (JCheckBox)source;
                    if (widget.isSelected())
                    {
                        core.setProperty(pf, propertyName, 1);
                    }
                    else
                    {
                        core.setProperty(pf, propertyName, 0);                          
                    }
                }
                else
                {
                    // Unknown so ignore it
                }

                core.setProperty(plugin_.DEVICE_NAME, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);

                updateValues();

                core.updateCoreProperties();
                core.updateSystemStateCache();
            }
            catch (Exception ex)
            {
                // All exceptions need the same basic response
                try
                {
                    // Ensure PureFocus is not left open for changes
                    core.setProperty(plugin_.DEVICE_NAME, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);

                    // If something went wrong, update widget value
                    if (source.getClass() == JTextField.class)
                    {
                        JTextField widget = (JTextField)source;
                        widget.setText(core.getProperty(pf, propertyName));
                    }
                    else if (source.getClass() == JCheckBox.class)
                    {
                        JCheckBox widget = (JCheckBox)source;
                        widget.setSelected(Integer.valueOf(core.getProperty(pf, propertyName)) != 0);
                    }
                    else
                    {
                        // Unknown so ignore it
                    }
                }
                catch (Exception e2)
                {
                    // These actions should not be able to fail
                }

                if (ex.getClass() == NumberFormatException.class)
                {
                    gui_.logs().showError("Value is not a number");
                }
                else
                {
                    gui_.logs().showError(ex.getMessage());
                }
            }
        }
	}
}
