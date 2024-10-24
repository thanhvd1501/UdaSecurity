import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import com.udacity.catpoint.security.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private SecurityService securityService;

    @Mock
    private ImageService imageService;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private StatusListener statusListener;

    private Sensor sensor;

    @BeforeEach
    void setup() {
        securityService = new SecurityService(securityRepository, imageService);
        sensor = createSensor();
    }

    private Sensor createSensor() {
        return new Sensor(UUID.randomUUID().toString(), SensorType.DOOR);
    }

    private Set<Sensor> getSensorList(boolean active, int number) {
        Set<Sensor> sensors = new HashSet<>();
        IntStream.range(0, number).forEach(i -> {
            Sensor sensor = new Sensor(UUID.randomUUID() + "_" + i, SensorType.DOOR);
            sensor.setActive(active);
            sensors.add(sensor);
        });
        return sensors;
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    @DisplayName("If alarm is armed and a sensor becomes activated, put the system into pending alarm status.")
    void systemArmedAndSensorInactivatedChangeToPending(ArmingStatus armingStatus) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    //Test2
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    @DisplayName("2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm on.")
    void SystemArmedAndSensorInactivated_PendingChangeToAlarm(ArmingStatus armingStatus){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        securityService.changeSensorActivationStatus(sensor,true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }
    //Test3
    @Test
    @DisplayName("3. If pending alarm and all sensors are inactive, return to no alarm state.")
    void SensorInActiveAndPendingAlarm_ChangeToNoAlarm() {
        Set<Sensor> allSensors = getSensorList(true, 5);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        allSensors.forEach(sensor -> securityService.changeSensorActivationStatus(sensor, false));
        verify(securityRepository, times(5)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //Test4
    @Test
    @DisplayName("Test when alarm is activated, no change in alarm status occurs while changing sensor activation")
    void AlarmActivated_NoChangeInAlarmStatusWhileChangeSensorActivation() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        testSensorActivationChange(true, false);


        reset(securityRepository);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        testSensorActivationChange(false, true);
    }

    private void testSensorActivationChange(boolean initialActiveState, boolean newActiveState) {
        sensor.setActive(initialActiveState);
        securityService.changeSensorActivationStatus(sensor, newActiveState);

        verify(securityRepository, times(1)).updateSensor(sensor);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    //Test5
    @Test
    @DisplayName("When sensor is activated and alarm status is pending, change alarm status to ALARM")
    void whenSensorActivatedAndAlarmPending_ThenChangeToAlarm(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor,true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Test6
    @Test
    @DisplayName("When sensor is already inactive, no change to alarm state should occur")
    void whenSensorAlreadyInactive_ThenNoChangeToAlarmState(){
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any());
    }


    @Test
    @DisplayName("When sensors are activated and alarm is ALARM with DISARMED status, change alarm to PENDING_ALARM")
    void whenSensorsActivatedAndAlarmIsActive_ThenChangeToPending(){

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);

        Set<Sensor> allSensors = getSensorList(true, 3);

        // Active all sensor
        allSensors.forEach(sensor -> securityService.changeSensorActivationStatus(sensor));

        verify(securityRepository,times(3)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }


    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    @DisplayName("When setting arming status to ARMED_HOME or ARMED_AWAY, no change in alarm status should occur")
    void whenSettingArmingStatusToArmed_ThenNoChangeInAlarmStatus(ArmingStatus armingStatus) {
        securityService.setArmingStatus(armingStatus);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    @DisplayName("When alarm status is PENDING and sensor is deactivated, change alarm status to NO_ALARM")
    void whenAlarmStatusIsPendingAndSensorDeactivated_ThenChangeToNoAlarm(){

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // Set sensor status is inactive
        sensor.setActive(false);

        securityService.changeSensorActivationStatus(sensor);

        verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    //Test7
    @Test
    @DisplayName("When a cat is identified and the system is ARMED_HOME, change alarm status to ALARM")
    void whenCatIdentifiedAndSystemIsArmedHome_ThenChangeAlarmStatusToAlarm(){

        // Set the dimensions for the cat image
        int width = 100;
        int height = 100;

        BufferedImage catImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(eq(catImage), anyFloat())).thenReturn(true);

        // Process the cat image
        securityService.processImage(catImage);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);

    }

    //Test8
    @Test
    @DisplayName("When a cat is not identified in the image, set alarm status to NO_ALARM")
    void whenCatNotIdentified_ThenSetAlarmStatusToNoAlarm(){
        int width = 100;
        int height = 100;
        BufferedImage catImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        sensor.setActive(false);
        when(imageService.imageContainsCat(eq(catImage), anyFloat())).thenReturn(false);

        securityService.processImage(catImage);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);

    }

    //Test 9
    @Test
    @DisplayName("When arming status is set to DISARMED, set alarm status to NO_ALARM")
    void whenArmingStatusSetToDisarmed_ThenSetAlarmStatusToNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    //Test10
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    @DisplayName("When the system is armed, reset all sensors to inactive")
    void whenSystemArmed_ThenResetAllSensorsToInactive(ArmingStatus armingStatus){

        Set<Sensor> sensors = getSensorList(true, 3);

        when(securityRepository.getSensors()).thenReturn(sensors);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.setArmingStatus(armingStatus);

        securityService.getSensors().forEach(sensor -> {
            assertFalse(sensor.getActive());
        });
    }
    //Test 11
    @Test
    @DisplayName("When a cat is identified and system is ARMED_HOME, change alarm status to ALARM")
    void whenCatIdentifiedAndSystemIsArmedHome_ThenChangeAlarmStatusToAlarm2() {
        int width = 100;
        int height = 100;

        BufferedImage catImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);

        securityService.processImage(catImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    @DisplayName("Add and remove a status listener successfully")
    void addAndRemoveStatusListener() {
        securityService.addStatusListener(statusListener);
        securityService.removeStatusListener(statusListener);
    }

    @Test
    @DisplayName("Add and remove a sensor successfully")
    void addAndRemoveSensor() {
        securityService.addSensor(sensor);
        securityService.removeSensor(sensor);
    }

}
