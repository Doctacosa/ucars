package com.useful.ucars;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.useful.ucars.controls.ControlScheme;
import com.useful.ucars.controls.ControlSchemeManager;
import com.useful.ucarsCommon.StatValue;

public class MotionManager {
	
	public static Vector rotateXZVector3dDegrees(Vector original, double degrees){
		double[] out = rotateVector2dRadians(original.getX(), original.getZ(), Math.toRadians(degrees));
		original.setX(out[0]);
		original.setZ(out[1]);
		return original;
	}
	
	public static double[] rotateVector2dDegrees(double x, double y, double degrees){
		return rotateVector2dRadians(x, y, Math.toRadians(degrees));
	}
	
	public static double[] rotateVector2dRadians(double x, double y, double radians){
	    double[] result = new double[2];
	    result[0] = x * Math.cos(radians) - y * Math.sin(radians);
	    result[1] = x * Math.sin(radians) + y * Math.cos(radians);
	    return result;
	}

	public static void move(Player player, float f, float s, boolean jumping) { // 'f' and 's' are values taken in by the vehicle control packet		
		Vector vec = new Vector();
		Entity ent = player.getVehicle();
		if (ent == null) {
			return;
		}
		while (!(ent instanceof Minecart) && ent.getVehicle() != null) {
			ent = ent.getVehicle();
		}
		if(!ucars.listener.inACar(player)){
		}
		if (!ucars.listener.inACar(player) || !(ent instanceof Minecart)) {
			return;
		}
		final Minecart car = (Minecart) ent;
		// Location loc = car.getLocation();
		// Vector carD = loc.getDirection();
		Vector plaD = player.getEyeLocation().getDirection();
		
		if(jumping){
			if(!player.hasMetadata("ucarsToggleControls")){
				player.setMetadata("ucarsToggleControls", new StatValue(true, ucars.plugin));
				ControlSchemeManager.toggleControlScheme(player);
				if(ControlSchemeManager.getScheme(player).equals(ControlScheme.KEYBOARD)){
					car.removeMetadata("ucarsSteeringDir", ucars.plugin);
					car.setMetadata("ucarsSteeringDir", new StatValue(plaD.normalize(), ucars.plugin));
				}
			}
		}
		else { // !jumping
			if(player.hasMetadata("ucarsToggleControls")){
				player.removeMetadata("ucarsToggleControls", ucars.plugin);
			}
		}
		
		ControlScheme controls = ControlSchemeManager.getScheme(player);
		boolean keyboardSteering = controls.equals(ControlScheme.KEYBOARD);
		
		Vector carDirection = null;
		if(keyboardSteering){
			try {
				if(car.hasMetadata("ucarsSteeringDir")){
					carDirection = (Vector) car.getMetadata("ucarsSteeringDir").get(0).value();
				}
			} catch (Exception e) {
				carDirection = null;
			}
			if(carDirection == null){
				carDirection = plaD.normalize();
			}
		}
		
		if (f == 0) {
			return;
		}
		Boolean forwards = true; // if true, forwards, else backwards
		int side = 0; // -1=left, 0=straight, 1=right
		Boolean turning = false;
		if (f < 0) {
			forwards = false;
		} else {
			forwards = true;
		}
		if (s > 0) {
			side = -1;
			turning = true;
		}
		if (s < 0) {
			side = 1;
			turning = true;
		}
		double y = -0.35; // rough gravity of minecraft
		double d = 27;
		Boolean doDivider = false;
		Boolean doAction = false;
		double divider = 0.5; // x of the (1) speed
		double rotMod = 5;
		if (turning) {
			if (side < 0) {// do left action
				if(!keyboardSteering){
					doAction = true;
					car.setMetadata("car.action", new StatValue(true, ucars.plugin));
				}
				else {
					carDirection = rotateXZVector3dDegrees(carDirection, -rotMod);
				}
			} else if (side > 0) {// do right action
				if(!keyboardSteering){
					doDivider = true;
					car.setMetadata("car.braking",
						new StatValue(true, ucars.plugin));
				}
				else {
					carDirection = rotateXZVector3dDegrees(carDirection, rotMod);
				}
			}
		}
		if(keyboardSteering){
			car.removeMetadata("ucarsSteeringDir", ucars.plugin);
			car.setMetadata("ucarsSteeringDir", new StatValue(carDirection.normalize(), ucars.plugin));
			plaD = carDirection;
		}
		if (forwards) { // Mouse controls please
			double x = plaD.getX() / d;
			double z = plaD.getZ() / d;
			if (!doDivider) {
				if (car.hasMetadata("car.braking")) {
					car.removeMetadata("car.braking", ucars.plugin);
				}
			}
			if(!doAction){
				if(car.hasMetadata("car.action")){
					car.removeMetadata("car.action", ucars.plugin);
				}
			}
			vec = new Vector(x, y, z);
			final ucarUpdateEvent event = new ucarUpdateEvent(car, vec, player);
			event.setDoDivider(doDivider);
			event.setDivider(divider);
			final Vector v = vec;
			ucars.plugin.getServer().getScheduler()
					.runTask(ucars.plugin, new Runnable() {

						public void run() {
							ControlInput.input(car, v, event);
						}
					});
			return;
		}
		if (!forwards) { // Mouse controls please
			double x = plaD.getX() / d;
			double z = plaD.getZ() / d;
			if (!doDivider) {
				if (car.hasMetadata("car.braking")) {
					car.removeMetadata("car.braking", ucars.plugin);
				}
			}
			x = 0 - x;
			z = 0 - z;
			vec = new Vector(x, y, z);
			final Vector v = vec;
			final ucarUpdateEvent event = new ucarUpdateEvent(car, vec, player);
			event.setDoDivider(doDivider);
			event.setDivider(divider);
			Bukkit.getScheduler().runTask(ucars.plugin, new Runnable(){

				@Override
				public void run() {
					ControlInput.input(car, v, event);
					return;
				}});
			return;
		}
	}

}
