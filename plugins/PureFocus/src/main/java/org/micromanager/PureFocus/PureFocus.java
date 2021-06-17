/**
@file PureFocus.java
@author Graham Bartlett
@copyright Prior Scientific Instruments Limited, 2021
@brief Micro-Manager plugin for control of the Prior PureFocus PF-850 autofocus unit

Micro-Manager plugin giving GUI control of the PF-850 setup and configuration.

Licensed under the BSD license.
*/

package org.micromanager.PureFocus;

import com.google.common.eventbus.Subscribe;

import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;

import org.micromanager.MenuPlugin;
import org.micromanager.Studio;
import org.micromanager.events.internal.DefaultSystemConfigurationLoadedEvent;

import mmcorej.CMMCore;
import mmcorej.StrVector;
import mmcorej.DeviceType;


/** Micro-Manager plugin for control of the Prior PureFocus PF-850 autofocus
 */
@Plugin(type = MenuPlugin.class)
@SuppressWarnings(value = {"serial", "static-access"})   
public class PureFocus implements MenuPlugin, SciJavaPlugin
{
    public static final String MENUNAME = "Prior PureFocus PF-850";
    public static final String TOOLTIPDESCRIPTION =
        "Control the Prior PureFocus PF-850 autofocus system";

    private Studio gui_;
    private PureFocusFrame myFrame_;
    
    // Common strings
    public static final String DEVICE_NAME = "PureFocus850";
    public static final String CONFIG_GROUP = "PureFocus850Group";
    public static final String CONFIG_GROUP_PRESET = "PureFocus850Preset";
    
    // Property names for enabling settings changes
    // NOTE: These must match the C++ device adapter property names
    public static final String CONFIG_IN_PROGRESS = "ConfigInProgress";
    public static final String SINGLE_CHANGE_IN_PROGRESS = "SingleChangeInProgress";
    
    // Objective settings names
    // NOTE: These must match the C++ device adapter property names
    public static final String OBJECTIVE_PREFIX = "Objective";
    public static final String CURRENT_PREFIX = "Current-";   
    public static final String PRESET = "Preset";
    public static final String LENS_OFFSET = "LensOffset";
    public static final String KP = "KP";
    public static final String KI = "KI";
    public static final String KD = "KD";     
    public static final String OUTPUT_LIMIT_MINIMUM = "OutputLimitMinimum";
    public static final String OUTPUT_LIMIT_MAXIMUM = "OutputLimitMaximum";
    public static final String SAMPLE_LOW_THRESHOLD = "SampleLowThreshold";
    public static final String FOCUS_LOW_THRESHOLD = "FocusLowThreshold";
    public static final String FOCUS_HIGH_THRESHOLD = "FocusHighThreshold";
    public static final String FOCUS_RANGE_THRESHOLD = "FocusRangeThreshold";
    public static final String INTERFACE_HIGH_THRESHOLD = "InterfaceHighThreshold";
    public static final String INTERFACE_LOW_THRESHOLD = "InterfaceLowThreshold";
    public static final String LASER_POWER = "LaserPower";   
    public static final String BACKGROUND_A = "BackgroundA";
    public static final String BACKGROUND_B = "BackgroundB";
    public static final String BACKGROUND_C = "BackgroundC";
    public static final String BACKGROUND_D = "BackgroundD";
    public static final String REGION_START_D = "RegionStartD";
    public static final String REGION_END_D = "RegionEndD";    
    public static final String PINHOLE_CENTRE = "PinholeCentre";
    public static final String PINHOLE_WIDTH = "PinholeWidth";
    public static final String IS_SERVO_LIMIT_ON = "IsServoLimitOn";
    public static final String SERVO_LIMIT_MAXIMUM_POSITIVE = "ServoLimitMaxPositiveMicrons";
    public static final String SERVO_LIMIT_MAXIMUM_NEGATIVE = "ServoLimitMaxNegativeMicrons";

    // Global settings names
    // NOTE: These must match the C++ device adapter property names
    public static final String GLOBAL_PREFIX = "Global-";
    public static final String IS_PIEZO_MOTOR = "Global-IsPiezoMotor";
    public static final String SERVO_ON = "Global-ServoOn";
    public static final String SERVO_INHIBIT = "Global-ServoInhibit";
    public static final String FOCUS_INTERRUPT_ON = "Global-FocusInterruptOn";
    public static final String INTERFACE_INHIBIT = "Global-InterfaceInhibit";
    public static final String INTERFACE_INHIBIT_COUNT = "Global-InterfaceInhibitCount";
    public static final String DIGIPOT_CONTROLS_OFFSET = "Global-DigipotControlsOffset";
    public static final String IS_SERVO_DIRECTION_POSITIVE = "Global-IsServoDirectionPositive";   
    public static final String IS_FOCUS_DRIVE_DIRECTION_POSITIVE = "Global-IsFocusDriveDirectionPositive";
    public static final String EXPOSURE_TIME_US = "Global-ExposureTimeUs";
    public static final String DIGIPOT_OFFSET_SPEED_PERCENT = "Global-DigipotOffsetSpeedPercent";
    public static final String FOCUS_DRIVE_RANGE_MICRONS = "Global-FocusDriveRangeMicrons";
    public static final String IN_FOCUS_RECOVERY_TIME_MS = "Global-InFocusRecoveryTimeMs";

    // Names of volatile settings not saved
    // NOTE: These must match the C++ device adapter property names
    public static final String OBJECTIVE = "Setting-Objective";
    public static final String OFFSET_POSITION_MICRONS = "Setting-OffsetPositionMicrons";
    public static final String FOCUS_POSITION_MICRONS = "Setting-FocusPositionMicrons";

    // Names of status values read back
    // NOTE: These must match the C++ device adapter property names    
    public static final String CALCULATION_ABCD = "Status-CalculationABCD";
    public static final String FOCUS_PID_TARGET = "Status-FocusPidTarget";
    public static final String FOCUS_PID_POSITION = "Status-FocusPidPosition";
    public static final String FOCUS_PID_ERROR = "Status-FocusPidError";
    public static final String FOCUS_PID_OUTPUT = "Status-FocusPidOutput";
    public static final String FOCUS_STATE = "Status-FocusState";
    public static final String TIME_TO_IN_FOCUS = "Status-TimeToInFocus";
    public static final String IS_OFFSET_MOVING = "Status-IsOffsetMoving";
    public static final String IS_FOCUS_DRIVE_MOVING = "Status-IsFocusDriveMoving";
    public static final String POSITIVE_LIMIT_SWITCH = "Status-PositiveLimitSwitch";
    public static final String NEGATIVE_LIMIT_SWITCH = "Status-NegativeLimitSwitch";  
    
    // Names of other properties
    // NOTE: These must match the C++ device adapter property names
    public static final String SERIAL_NUMBER = "SerialNumber";
    public static final String FIRMWARE_BUILD_VERSION = "FirmwareBuildVersion";
    public static final String FIRMWARE_BUILD_DATE_TIME = "FirmwareBuildDateTime";
    public static final String ARRAY_READ_INDEX = "ArrayReadIndex";
    public static final String OBJECTIVE_PRESET_NAMES = "ObjectivePresetNames";
    
    
    @Override
    public void setContext(Studio app)
    {
        gui_ = app;
        gui_.getEventManager().registerForEvents(this);
    }

    
    @Override
    public String getSubMenu()
    {
        return "Device Control";
    }

    
    /** Event triggered when a configuration file is loaded or set up.
     * 
     * PF-850 settings are saved in the configuration file, but are not automatically
     * loaded (because they are stored in a config group preset and not pre-init 
     * settings).  We need to have the plugin force the load of configuration 
     * settings from the config group preset.
     * 
     * If this is not already in the configuration file, settings must be set to
     * defaults.  A config group must also be created with these default settings
     * as a preset, so that saving the configuration file will save the settings.
     * 
     * Most users will not have a PF-850 fitted, so we do nothing if we do not
     * have one in the system.
     * 
     * @param event Event
     */
    @Subscribe
    public void onConfigurationLoadedEvent(DefaultSystemConfigurationLoadedEvent event)
    {
		CMMCore core = gui_.getCMMCore();
		StrVector afs = core.getLoadedDevicesOfType(DeviceType.AutoFocusDevice);
		boolean found = false;
		for (String af : afs)
		{
			try
			{
                if (af.equals(DEVICE_NAME))
				{
					found = true;
					break;
				}
			}
			catch (Exception ex)
			{
				gui_.logs().logError(ex);
			}
		}
        
        if (found)
        {
            // We have a PF-850.
            // This event is invoked twice when loading a configuration
            // file, and is also invoked when closing the application for some
            // reason.  Pushing settings to the PF-850 takes significant time
            // (around 5s) so we only do this the first time and ignore any
            // subsequent times.
            boolean configAlreadySet = false;
            try
            {
                String currentConfigPreset = core.getCurrentConfig(CONFIG_GROUP);
                if (currentConfigPreset.equals(CONFIG_GROUP_PRESET))
                {
                    configAlreadySet = true;
                }
            }
            catch (Exception e)
            {
                // Config group does not exist
            }
            
            if (!configAlreadySet)
            {
                try
                {
                    // Get the device ready for the new configuration
                    core.setProperty(DEVICE_NAME, CONFIG_IN_PROGRESS, 1);
                }
                catch (Exception e)
                {
                    // Never fails
                }
                
                try
                {
                    // Load in the PureFocus configuration
                    core.setConfig(CONFIG_GROUP, CONFIG_GROUP_PRESET);
                }
                catch (Exception e)
                {
                    // Create configuration
                    try
                    {
                        core.defineConfigGroup(CONFIG_GROUP);        
                    }
                    catch (Exception e2)
                    {
                        // May happen if config group already exists, in which case it's already OK
                    }
                    
                    try
                    {
                        core.defineConfig(CONFIG_GROUP, CONFIG_GROUP_PRESET);           
                    }
                    catch (Exception e2)
                    {
                        // Should never fail
                    }                    
                }
                
                try
                {
                    // If a config group was just created, we must push its values
                    // back to the configuration.  Even if a config group existed though,
                    // we cannot assume that the configuration file has a complete set
                    // of parameters (especially if the plugin has changed to add more),
                    // so push all parameters back to the configuration.  Values have
                    // not changed during this, so existing values will remain the same.
                    StrVector propertyNames = core.getDevicePropertyNames(DEVICE_NAME);
                    for(String propertyName: propertyNames)
                    {
                        if (propertyName.startsWith(OBJECTIVE_PREFIX + "1") ||
                            propertyName.startsWith(OBJECTIVE_PREFIX + "2") ||
                            propertyName.startsWith(OBJECTIVE_PREFIX + "3") ||
                            propertyName.startsWith(OBJECTIVE_PREFIX + "4") ||
                            propertyName.startsWith(OBJECTIVE_PREFIX + "5") ||
                            propertyName.startsWith(OBJECTIVE_PREFIX + "6") ||
                            propertyName.startsWith(GLOBAL_PREFIX))
                        {
                            String value = core.getProperty(DEVICE_NAME, propertyName);
                            core.defineConfig(CONFIG_GROUP, CONFIG_GROUP_PRESET, DEVICE_NAME, propertyName, value);
                        }
                    }
                }
                catch (Exception e)
                {
                    // Should never fail
                }                  
            }
            
            try
            {
                // Once the configuration is loaded, enable normal running
                core.setProperty(DEVICE_NAME, CONFIG_IN_PROGRESS, 0);
            }
            catch (Exception e)
            {
                // Never fails
            }
            
            if (myFrame_ != null)
            {
                // If the plugin is already open, force an update to display
                // new settings.
                myFrame_.updateValues(true);
            }
        }
        else
        {
            // Close anything left open from previous run
            try
            {
                if (myFrame_ != null)
                {
                    myFrame_.dispose();
                }
            }
            catch (Exception e)
            {
                // Should never fail
            }             
            
            myFrame_ = null;            
        }
    }
   

    @Override
    public void onPluginSelected()
    {
        CMMCore core = gui_.getCMMCore();
 		StrVector afs = core.getLoadedDevicesOfType(DeviceType.AutoFocusDevice);
		boolean found = false;
		for (String af : afs)
		{
			try
			{
                if (af.equals(DEVICE_NAME))
				{
					found = true;
					break;
				}
			}
			catch (Exception ex)
			{
				gui_.logs().logError(ex);
			}
		}
        
		if (!found)
		{
			gui_.logs().showError("This plugin needs the Prior PureFocus PF-850 autofocus");
			throw new IllegalArgumentException("This plugin needs the Prior PureFocus PF-850 autofocus");
		}        
        
        if (myFrame_ == null)
        {
            try
            {
                myFrame_ = new PureFocusFrame(gui_, this);
            }
            catch (Exception e)
            {
                gui_.logs().logError(e);
                return;
            }
        }
      
        myFrame_.setVisible(true);
    }

    
    public void tellFrameClosed()
    {
        myFrame_ = null;
    }
   
    
    @Override
    public String getName()
    {
        return MENUNAME;
    }

    
    @Override
    public String getHelpText()
    {
        return TOOLTIPDESCRIPTION;
    }

    
    @Override
    public String getVersion()
    {
        return "0.01";
    }

    
    @Override
    public String getCopyright()
    {
        return "Prior Scientific Instruments Limited, 2021";
    }
}
