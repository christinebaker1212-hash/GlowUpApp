package com.glowup.app;

import java.util.HashMap;
import java.util.Map;

/** TRAINING ARC's reasons for each move (GLOW UP uses the ones in Plan.ALL). */
final class FlavorText {
    private FlavorText() {}

    static final Map<String, String> WHY_M = new HashMap<>();

    private static void w(String id, String why) {
        WHY_M.put(id, why);
    }

    static {
        // legs & glutes: POWER
        w("glute_bridge", "Strong glutes protect your lower back and power every jump, sprint and squat.");
        w("couch_thrust", "Hip thrusts build the strongest muscle in your body. More muscle, more fat burned at rest.");
        w("sl_bridge", "Twice the load per side with no equipment, and it fixes left / right imbalances.");
        w("sl_thrust", "Heavy glute work without a barbell: athletic power from the hips.");
        w("chair_squat", "The base of leg strength. Big legs burn big energy.");
        w("squat", "The king of leg moves: legs are half your muscle, and training them drives fat loss.");
        w("reverse_lunge", "One leg at a time: strength, balance and fewer knee complaints.");
        w("split_squat", "One of the hardest bodyweight leg moves there is. Builds legs that match the upper body.");
        w("sumo_squat", "Inner thighs and glutes, and a deep, strong stance.");
        w("curtsy_lunge", "Hits the side of the hips that keeps your knees stable when you move fast.");
        w("step_up", "Leg strength plus conditioning, and stairs stop being a thing.");
        w("calf_raise", "Stronger ankles and calves for running, jumping and fighting stance.");
        w("clamshell", "Fires up the side glute that keeps your knees and hips healthy under load.");
        w("side_leg_raise", "Hip stability: the hidden muscle behind strong legs and a solid stance.");
        w("fire_hydrant", "Hip strength and mobility for kicks, lunges and squats.");
        w("standing_abduction", "Side-hip strength plus balance in one.");
        w("donkey_kick", "Glute power without loading your back.");
        w("frog_pump", "Glute-only work that wakes up your hips for the bigger moves.");
        w("glute_kickback", "Hip extension power: the muscle behind sprinting and kicking.");
        w("jump_squat", "Explosive power: fast-twitch muscle and a big calorie burn.");
        // upper body: STRENGTH
        w("wall_pushup", "Perfect form first. Every strong press starts here.");
        w("counter_pushup", "Closer to the floor: more chest and triceps every rep.");
        w("knee_pushup", "Full push-up mechanics with less load. Build reps here, then drop the knees.");
        w("pushup", "Chest, shoulders, triceps and core in one move. Strict reps beat sloppy volume.");
        w("decline_pushup", "Feet up shifts the work to the upper chest and shoulders: the top of the V.");
        w("diamond_pushup", "Triceps make up most of your arm. This is how you grow them with no weights.");
        w("pike_pushup", "Shoulders build the width at the top of the V-taper.");
        w("door_row", "Pulling balances all that pushing: a strong back pulls the shoulders back and widens the frame.");
        w("table_row", "The lats and upper back: the muscles that make the V-shape.");
        w("wall_angel", "Opens a tight chest and pulls the shoulders back: instantly broader-looking.");
        w("prone_ytw", "Upper-back strength that fixes rounded shoulders.");
        w("superman", "Lower-back strength that keeps you injury-proof.");
        w("chair_dip_bent", "Triceps: two thirds of your upper arm.");
        w("chair_dip", "Heavier triceps and chest work.");
        // core
        w("wall_plank", "Learn to brace: the base of every strong lift.");
        w("counter_plank", "Closer to the floor, more core.");
        w("knee_plank", "Real core strength with less load.");
        w("plank", "A rock-solid core holds everything together. Abs show when the fat comes off; this builds them.");
        w("plank_taps", "Anti-rotation strength: the core that absorbs a hit.");
        w("knee_side_plank", "Obliques and side hips.");
        w("side_plank", "Obliques: the sides of the six-pack.");
        w("dead_bug", "Deep core control that protects your back.");
        w("bird_dog", "Core and back stability with no strain.");
        w("reverse_crunch", "Lower abs, the hardest part of the six-pack to build.");
        w("vacuum", "Trains the deep corset muscle that pulls the waist in tight.");
        // control
        w("tandem_stance", "Balance is a trainable skill. It starts here.");
        w("single_leg_stand", "Ankle, knee and hip control: the base of agility.");
        w("sl_stand_closed", "Elite-level balance.");
        // stamina
        w("march", "Heart and lungs, and steady fat burn.");
        w("dance", "Cardio that doesn't feel like cardio.");
        w("step_jacks", "Low-impact conditioning that's easy on the knees.");
        w("stair_climb", "Legs and lungs together.");
        w("high_knees", "High-intensity cardio for when you're ready.");
        w("shadow_box", "Real fight conditioning: shoulders, core and lungs, and the fastest-feeling minutes of the workout.");
        w("mountain_climber", "Conditioning and core at once: fat burn with ab work built in.");
        w("burpee", "The ultimate conditioning test. Every round makes the next one easier.");
    }
}
