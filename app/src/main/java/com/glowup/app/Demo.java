package com.glowup.app;

import java.util.ArrayList;
import java.util.List;

/**
 * Exercise demonstrations: a figure built from joints, posed in keyframes and animated between them.
 *
 * World units are metres, y up, the floor at y = 0, the figure facing right. Segment angles are absolute
 * directions: 0 = straight down, 90 = right (forward), 180 = up, -90 = left. Feet and hands are usually placed by
 * two-bone IK (footTo / handTo) so they stay planted while the body moves.
 */
final class Demo {
    private Demo() {}

    // body (metres)
    static final double TOR = 0.50, NECK = 0.07, HEAD = 0.105, UA = 0.29, FA = 0.27, TH = 0.44, SH = 0.43,
        FOOT = 0.16, SHW = Flavor.MALE ? 0.205 : 0.17, HPW = 0.10, AH = 0.08, WAIST = Flavor.MALE ? 0.15 : 0.12;
    static final int SIDE = 0, FRONT = 1;
    // what lights up
    static final int GLUTE = 1, THIGH = 2, SHIN = 4, CORE = 8, CHEST = 16, ARM = 32, BACK = 64, HIPSIDE = 128,
        SHOULDER = 256, INNER = 512, HEART = 1024;
    // props: {type, x0, x1, y}
    static final int WALL = 1, CHAIR = 2, COUCH = 3, COUNTER = 4, STEP = 5, MAT = 6, STAIRS = 7, TABLE = 8, DOOR = 9;

    static final class Pose {
        double x, y, t = 180, h, bend;
        double[] arm = new double[4];   // near upper, near fore, far upper, far fore
        double[] leg = new double[4];   // near thigh, near shin, far thigh, far shin
        double[] foot = {Double.NaN, Double.NaN};
        double ts = 1;                  // torso length (foreshortening)
        double[] ls = {1, 1, 1, 1};     // leg segment scales
        double[] as = {1, 1, 1, 1};     // arm segment scales
        boolean nearBehind;             // draw the near leg behind the body (front view crossing)
        boolean eyesClosed;
        String label = "";
        double move = 1.0, hold = 0.3;

        Pose copy() {
            Pose p = new Pose();
            p.x = x; p.y = y; p.t = t; p.h = h; p.bend = bend;
            p.arm = arm.clone(); p.leg = leg.clone(); p.foot = foot.clone(); p.ts = ts; p.ls = ls.clone(); p.as = as.clone();
            p.nearBehind = nearBehind; p.eyesClosed = eyesClosed; p.label = label; p.move = move; p.hold = hold;
            return p;
        }
    }

    static final class Anim {
        int view = SIDE, parts;
        boolean mirror, noFloor;
        final List<Pose> keys = new ArrayList<>();
        final List<double[]> props = new ArrayList<>();
        String caption = "";

        double cycle() {
            double s = 0;
            for (Pose p : keys) s += p.move + p.hold;
            return s;
        }
    }

    // ---------------- geometry ----------------

    static double[] dir(double a) {
        double r = Math.toRadians(a);
        return new double[]{Math.sin(r), -Math.cos(r)};
    }

    static double angleOf(double vx, double vy) {
        return Math.toDegrees(Math.atan2(vx, -vy));
    }

    static double[] add(double[] p, double len, double a) {
        double[] d = dir(a);
        return new double[]{p[0] + len * d[0], p[1] + len * d[1]};
    }

    /** Two-bone IK: {upper angle, lower angle} reaching from root toward target; bend +1 / -1 picks the side. */
    static double[] ik(double[] root, double[] target, double l1, double l2, int bend) {
        double vx = target[0] - root[0], vy = target[1] - root[1];
        double d = Math.max(1e-6, Math.min(l1 + l2 - 1e-4, Math.hypot(vx, vy)));
        double base = angleOf(vx, vy);
        double c = (l1 * l1 + d * d - l2 * l2) / (2 * l1 * d);
        double a = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, c))));
        double up = base + bend * a;
        double[] mid = add(root, l1, up);
        double lo = angleOf(target[0] - mid[0], target[1] - mid[1]);
        return new double[]{up, lo};
    }

    /** All joints of a pose. */
    static final class J {
        double[] hip, hipN, hipF, sh, shN, shF, neck, head, elN, waN, elF, waF, knN, anN, toN, knF, anF, toF, waist, waistN, waistF;
        double belly, headDir;
    }

    static J joints(Pose p, int view) {
        J j = new J();
        j.hip = new double[]{p.x, p.y};
        j.sh = add(j.hip, TOR * p.ts, p.t);
        double sw = view == FRONT ? SHW : 0, hw = view == FRONT ? HPW : 0;
        j.shN = add(j.sh, sw, p.t - 90);
        j.shF = add(j.sh, sw, p.t + 90);
        j.hipN = add(j.hip, hw, p.t - 90);
        j.hipF = add(j.hip, hw, p.t + 90);
        j.waist = add(j.hip, TOR * p.ts * 0.45, p.t);
        j.waistN = add(j.waist, view == FRONT ? WAIST : 0, p.t - 90);
        j.waistF = add(j.waist, view == FRONT ? WAIST : 0, p.t + 90);
        j.headDir = p.t + p.h;
        j.neck = add(j.sh, NECK, j.headDir);
        j.head = add(j.neck, HEAD, j.headDir);
        j.belly = p.t - 90;
        j.elN = add(j.shN, UA * p.as[0], p.arm[0]);
        j.waN = add(j.elN, FA * p.as[1], p.arm[1]);
        j.elF = add(j.shF, UA * p.as[2], p.arm[2]);
        j.waF = add(j.elF, FA * p.as[3], p.arm[3]);
        j.knN = add(j.hipN, TH * p.ls[0], p.leg[0]);
        j.anN = add(j.knN, SH * p.ls[1], p.leg[1]);
        j.knF = add(j.hipF, TH * p.ls[2], p.leg[2]);
        j.anF = add(j.knF, SH * p.ls[3], p.leg[3]);
        if (view == FRONT) {
            j.toN = add(j.anN, 0.08, (Double.isNaN(p.foot[0]) ? p.leg[1] + 75 : p.foot[0]));
            j.toF = add(j.anF, 0.08, (Double.isNaN(p.foot[1]) ? p.leg[3] - 75 : p.foot[1]));
        } else {
            j.toN = add(j.anN, FOOT, Double.isNaN(p.foot[0]) ? p.leg[1] + 90 : p.foot[0]);
            j.toF = add(j.anF, FOOT, Double.isNaN(p.foot[1]) ? p.leg[3] + 90 : p.foot[1]);
        }
        return j;
    }

    static Pose lerp(Pose a, Pose b, double u) {
        Pose p = a.copy();
        p.x = a.x + (b.x - a.x) * u;
        p.y = a.y + (b.y - a.y) * u;
        p.t = a.t + (b.t - a.t) * u;
        p.h = a.h + (b.h - a.h) * u;
        p.bend = a.bend + (b.bend - a.bend) * u;
        p.ts = a.ts + (b.ts - a.ts) * u;
        for (int i = 0; i < 4; i++) {
            p.arm[i] = a.arm[i] + (b.arm[i] - a.arm[i]) * u;
            p.leg[i] = a.leg[i] + (b.leg[i] - a.leg[i]) * u;
            p.ls[i] = a.ls[i] + (b.ls[i] - a.ls[i]) * u;
            p.as[i] = a.as[i] + (b.as[i] - a.as[i]) * u;
        }
        for (int i = 0; i < 2; i++) {
            double fa = Double.isNaN(a.foot[i]) ? defFoot(a, i) : a.foot[i];
            double fb = Double.isNaN(b.foot[i]) ? defFoot(b, i) : b.foot[i];
            p.foot[i] = Double.isNaN(a.foot[i]) && Double.isNaN(b.foot[i]) ? Double.NaN : fa + (fb - fa) * u;
        }
        p.nearBehind = u < 0.5 ? a.nearBehind : b.nearBehind;
        p.eyesClosed = u < 0.5 ? a.eyesClosed : b.eyesClosed;
        p.label = b.label;
        return p;
    }

    private static double defFoot(Pose p, int i) {
        return p.leg[i * 2 + 1] + 90;
    }

    /** The pose at time s (seconds) into the loop, and the label to show. */
    static Pose at(Anim a, double s) {
        int n = a.keys.size();
        if (n == 1) return a.keys.get(0);
        double cyc = a.cycle();
        s = ((s % cyc) + cyc) % cyc;
        for (int i = 0; i < n; i++) {
            Pose to = a.keys.get((i + 1) % n), from = a.keys.get(i);
            double seg = to.move + to.hold;
            if (s < seg) {
                if (s >= to.move) return to;
                double u = s / to.move;
                u = u * u * (3 - 2 * u);
                return lerp(from, to, u);
            }
            s -= seg;
        }
        return a.keys.get(0);
    }

    // ---------------- building ----------------

    static final class A {
        final Anim a = new Anim();

        A(int view, int parts) {
            a.view = view;
            a.parts = parts;
        }

        A prop(int type, double x0, double x1, double y) {
            a.props.add(new double[]{type, x0, x1, y});
            return this;
        }

        A mirror() { a.mirror = true; return this; }
        A noFloor() { a.noFloor = true; return this; }

        B k() {
            return new B(this);
        }

        Anim done() {
            return a;
        }
    }

    static final class B {
        final A owner;
        final Pose p = new Pose();

        B(A owner) {
            this.owner = owner;
            p.x = 0;
            p.y = TH + SH + AH;
            boolean front = owner.a.view == FRONT;
            p.arm = front ? new double[]{12, 8, -12, -8} : new double[]{4, 10, -4, 4};
        }

        int view() { return owner.a.view; }

        B at(double x, double y) { p.x = x; p.y = y; return this; }
        B t(double t) { p.t = t; return this; }
        B h(double h) { p.h = h; return this; }
        B bend(double b) { p.bend = b; return this; }
        B ts(double s) { p.ts = s; return this; }

        /** Put the shoulder (centre) at a point, keeping the torso angle. */
        B sh(double x, double y) {
            double[] d = dir(p.t);
            p.x = x - TOR * p.ts * d[0];
            p.y = y - TOR * p.ts * d[1];
            return this;
        }

        B arm(int n, double u, double f) { p.arm[n * 2] = u; p.arm[n * 2 + 1] = f; return this; }
        B arms(double u, double f) { return arm(0, u, f).arm(1, u, f); }
        B leg(int n, double th, double sh) { p.leg[n * 2] = th; p.leg[n * 2 + 1] = sh; return this; }
        B legs(double th, double sh) { return leg(0, th, sh).leg(1, th, sh); }
        B foot(int n, double a) { p.foot[n] = a; return this; }
        B shin(int n, double a) { p.leg[n * 2 + 1] = a; return this; }

        /** Point the thigh at a knee position (kneeling), stretching it a little if needed to land there. */
        B kneeTo(int n, double x, double y) {
            J j = joints(p, view());
            double[] r = n == 0 ? j.hipN : j.hipF;
            p.leg[n * 2] = angleOf(x - r[0], y - r[1]);
            p.ls[n * 2] = Math.max(0.85, Math.min(1.15, Math.hypot(x - r[0], y - r[1]) / TH));
            return this;
        }
        B legScale(int n, double th, double sh) { p.ls[n * 2] = th; p.ls[n * 2 + 1] = sh; return this; }
        B armScale(int n, double u, double f) { p.as[n * 2] = u; p.as[n * 2 + 1] = f; return this; }
        B behind() { p.nearBehind = true; return this; }
        B eyesClosed() { p.eyesClosed = true; return this; }

        B footTo(int n, double x, double y, int bend) {
            J j = joints(p, view());
            double[] r = ik(n == 0 ? j.hipN : j.hipF, new double[]{x, y}, TH * p.ls[n * 2], SH * p.ls[n * 2 + 1], bend);
            return leg(n, r[0], r[1]);
        }

        B feetTo(double x, double y) { return footTo(0, x, y, 1).footTo(1, x, y, 1); }

        B handTo(int n, double x, double y, int bend) {
            J j = joints(p, view());
            double[] r = ik(n == 0 ? j.shN : j.shF, new double[]{x, y}, UA * p.as[n * 2], FA * p.as[n * 2 + 1], bend);
            return arm(n, r[0], r[1]);
        }

        B handsTo(double x, double y, int bend) { return handTo(0, x, y, bend).handTo(1, x, y, bend); }

        /** Hands on hips (elbows out / back). */
        B handsOnHips() {
            J j = joints(p, view());
            if (view() == FRONT) {
                handTo(0, j.hipN[0] + 0.05, j.hipN[1] + 0.06, 1);
                return handTo(1, j.hipF[0] - 0.05, j.hipF[1] + 0.06, -1);
            }
            double[] w = Demo.add(Demo.add(j.hip, 0.08, p.t), 0.02, j.belly);
            return handsTo(w[0], w[1], -1);
        }

        /** A straight body from the ankles along torso angle t (planks, push-ups): legs continue the torso line. */
        B line(double ax, double ay, double t) {
            p.t = t;
            double[] d = dir(t);
            p.x = ax + (TH + SH) * d[0];
            p.y = ay + (TH + SH) * d[1];
            p.leg[0] = p.leg[2] = t + 180;
            p.leg[1] = p.leg[3] = t + 180;
            p.ls = new double[]{1, 1, 1, 1};
            return this;
        }

        /** A straight body from the knees (knee push-ups, knee planks), shins at the given angle. */
        B lineKnee(double kx, double ky, double t, double shin) {
            p.t = t;
            double[] d = dir(t);
            p.x = kx + TH * d[0];
            p.y = ky + TH * d[1];
            p.leg[0] = p.leg[2] = t + 180;
            p.leg[1] = p.leg[3] = shin;
            p.ls = new double[]{1, 1, 1, 1};
            return this;
        }

        /** Thigh at a set angle, shin reaching a foot point (stretching / shrinking the shin to land there). */
        B thighFoot(int n, double thigh, double fx, double fy) {
            p.leg[n * 2] = thigh;
            J j = joints(p, view());
            double[] k = n == 0 ? j.knN : j.knF;
            p.leg[n * 2 + 1] = angleOf(fx - k[0], fy - k[1]);
            p.ls[n * 2 + 1] = Math.hypot(fx - k[0], fy - k[1]) / SH;
            return this;
        }

        J now() { return joints(p, view()); }

        /** Drop (or raise) the whole body so its lowest point rests on the floor. */
        B ground() {
            J j = joints(p, view());
            double low = 1e9;
            double[][] pts = {j.anN, j.anF, j.knN, j.knF, j.elN, j.elF, j.waN, j.waF, j.hip, j.sh, j.head, j.toN, j.toF};
            double[] off = {0.06, 0.06, 0.06, 0.06, 0.05, 0.05, 0.04, 0.04, 0.10, 0.08, HEAD, 0.03, 0.03};
            for (int i = 0; i < pts.length; i++) low = Math.min(low, pts[i][1] - off[i]);
            p.y -= low;
            return this;
        }

        B label(String s) { p.label = s; return this; }
        B move(double s) { p.move = s; return this; }
        B hold(double s) { p.hold = s; return this; }

        /** Finish this keyframe and add it. */
        A add() {
            owner.a.keys.add(p);
            return owner;
        }

        /** Finish, add, and start the next keyframe as a copy of this one. */
        B next() {
            owner.a.keys.add(p);
            B b = new B(owner);
            b.p.x = p.x; b.p.y = p.y; b.p.t = p.t; b.p.h = p.h; b.p.bend = p.bend; b.p.ts = p.ts;
            b.p.arm = p.arm.clone(); b.p.leg = p.leg.clone(); b.p.foot = p.foot.clone();
            b.p.ls = p.ls.clone(); b.p.as = p.as.clone(); b.p.nearBehind = p.nearBehind;
            b.p.move = p.move; b.p.hold = p.hold;
            return b;
        }
    }

    static final double STAND = TH + SH + AH;

    // ---------------- the demonstrations ----------------

    static Anim get(String id) {
        switch (id) {
            // ===== mobility =====
            case "cat_cow": {
                A a = new A(SIDE, BACK).prop(MAT, -0.6, 0.8, 0);
                a.k().at(0, 0.50).t(82).h(-40).bend(0.06).kneeTo(0, 0.0, 0.06).kneeTo(1, 0.0, 0.06).shin(0, -90).shin(1, -90)
                    .foot(0, -170).foot(1, -170).handsTo(0.50, 0.04, 1).label("ROUND YOUR BACK, BREATHE OUT").move(1.6).hold(0.8)
                    .next().bend(-0.05).h(30).handsTo(0.50, 0.04, 1).label("LET IT SINK, LOOK UP, BREATHE IN").add();
                return a.done();
            }
            case "hip_circles": {
                A a = new A(FRONT, GLUTE | HIPSIDE | CORE);
                double[][] c = {{-0.07, 0.945, 186}, {0, 0.93, 180}, {0.07, 0.945, 174}, {0, 0.96, 180}};
                B b = a.k();
                for (int i = 0; i < 4; i++) {
                    b.at(c[i][0], c[i][1]).t(c[i][2]).footTo(0, 0.14, AH, 1).footTo(1, -0.14, AH, -1).handsOnHips()
                        .label("BIG, SLOW CIRCLES").move(0.7).hold(0);
                    if (i < 3) b = b.next(); else b.add();
                }
                return a.done();
            }
            case "arm_circles": {
                A a = new A(FRONT, SHOULDER);
                double[] u = {82, 92, 100, 92};
                double[] f = {-8, -14, -4, 6};
                B b = a.k();
                for (int i = 0; i < 4; i++) {
                    b.footTo(0, 0.14, AH, 1).footTo(1, -0.14, AH, -1).arm(0, u[i], u[i] + f[i]).arm(1, -u[i], -u[i] - f[i])
                        .label(i < 2 ? "SMALL CIRCLES" : "...GETTING BIGGER").move(0.45).hold(0);
                    if (i < 3) b = b.next(); else b.add();
                }
                return a.done();
            }
            case "lunge_reach": {
                A a = new A(SIDE, THIGH | CORE).prop(MAT, -0.9, 0.7, 0);
                a.k().at(0, STAND).footTo(0, 0.05, AH, 1).footTo(1, -0.05, AH, 1).label("STAND TALL").move(1.2).hold(0.4)
                    .next().at(-0.06, 0.60).t(194).arms(172, 178).footTo(0, 0.40, AH, 1).footTo(1, -0.50, 0.14, 1).foot(1, 40)
                    .label("LUNGE, REACH UP AND BACK").hold(1.6).add();
                return a.done();
            }
            case "hip_flexor": {
                A a = new A(SIDE, THIGH | GLUTE).prop(MAT, -0.8, 0.7, 0);
                a.k().at(-0.10, 0.46).kneeTo(1, -0.22, 0.06).shin(1, -90).foot(1, -160)
                    .footTo(0, 0.36, AH, 1).handsOnHips().label("KNEEL TALL").move(1.4).hold(0.6)
                    .next().at(0.0, 0.44).kneeTo(1, -0.22, 0.06).shin(1, -90).foot(1, -160).footTo(0, 0.36, AH, 1).handsOnHips()
                    .label("SQUEEZE THE BACK GLUTE, SHIFT FORWARD").hold(2.0).add();
                return a.done();
            }
            case "figure4": {
                A a = new A(SIDE, GLUTE).prop(MAT, -0.9, 0.8, 0);
                B b = a.k().at(0, 0.10).t(-90).footTo(1, 0.40, AH, 1).arms(90, 90);
                J j = b.now();
                b.footTo(0, j.knF[0] - 0.02, j.knF[1] + 0.06, 1).label("CROSS ONE ANKLE OVER THE OTHER KNEE").move(1.4).hold(0.5);
                b = b.next().leg(1, 160, 95);
                j = b.now();
                b.footTo(0, j.knF[0] - 0.02, j.knF[1] + 0.06, 1).handsTo(j.knF[0] - 0.08, j.knF[1] - 0.12, 1)
                    .label("PULL THE BOTTOM LEG IN, FEEL THE STRETCH").hold(2.0).add();
                return a.done();
            }
            case "childs_pose": {
                A a = new A(SIDE, BACK).prop(MAT, -0.7, 1.0, 0);
                a.k().at(0.0, 0.50).kneeTo(0, 0.02, 0.06).kneeTo(1, 0.02, 0.06).shin(0, -90).shin(1, -90).foot(0, -170).foot(1, -170)
                    .label("KNEEL TALL").move(1.6).hold(0.4)
                    .next().at(-0.30, 0.20).t(72).h(-15).bend(0.04).kneeTo(0, 0.02, 0.06).kneeTo(1, 0.02, 0.06).shin(0, -90).shin(1, -90)
                    .arms(92, 90).label("SIT BACK ON YOUR HEELS, REACH FORWARD").hold(2.2).add();
                return a.done();
            }
            case "butterfly": {
                A a = new A(FRONT, INNER).prop(MAT, -0.8, 0.8, 0);
                a.k().at(0, 0.12).leg(0, 74, -100).leg(1, -74, 100).handsTo(0, 0.05, 1).label("SIT TALL").move(1.0).hold(0.4)
                    .next().leg(0, 82, -105).leg(1, -82, 105).handsTo(0, 0.05, 1).label("LET YOUR KNEES FALL OPEN").hold(0.8).add();
                return a.done();
            }

            // ===== CURVES =====
            case "glute_bridge": {
                A a = new A(SIDE, GLUTE | THIGH).prop(MAT, -0.9, 0.7, 0);
                a.k().at(0, 0.10).t(-90).footTo(0, 0.42, AH, 1).footTo(1, 0.40, AH, 1).arms(90, 90)
                    .label("FEET FLAT, KNEES BENT").move(1.0).hold(0.3)
                    .next().t(-62).sh(-0.47, 0.10).h(-28).footTo(0, 0.42, AH, 1).footTo(1, 0.40, AH, 1).arms(90, 90)
                    .label("SQUEEZE AND LIFT, PAUSE").hold(0.9).add();
                return a.done();
            }
            case "couch_thrust": {
                A a = new A(SIDE, GLUTE | THIGH).prop(COUCH, -1.2, -0.45, 0.42);
                a.k().t(-122).sh(-0.47, 0.47).h(12).footTo(0, 0.45, AH, 1).footTo(1, 0.43, AH, 1).arms(-90, -90)
                    .label("UPPER BACK ON THE COUCH").move(1.0).hold(0.3)
                    .next().t(-90).sh(-0.47, 0.47).h(-20).footTo(0, 0.45, AH, 1).footTo(1, 0.43, AH, 1).arms(-90, -90)
                    .label("DRIVE UP, SQUEEZE AT THE TOP").hold(0.9).add();
                return a.done();
            }
            case "sl_bridge": {
                A a = new A(SIDE, GLUTE).prop(MAT, -0.9, 0.9, 0);
                a.k().at(0, 0.10).t(-90).footTo(1, 0.42, AH, 1).leg(0, 125, 125).arms(90, 90)
                    .label("ONE FOOT DOWN, ONE LEG UP").move(1.0).hold(0.3)
                    .next().t(-62).sh(-0.47, 0.10).h(-28).footTo(1, 0.42, AH, 1).leg(0, 118, 118).arms(90, 90)
                    .label("LIFT, HIPS LEVEL").hold(0.9).add();
                return a.done();
            }
            case "sl_thrust": {
                A a = new A(SIDE, GLUTE).prop(COUCH, -1.2, -0.45, 0.42);
                a.k().t(-122).sh(-0.47, 0.47).h(12).footTo(1, 0.45, AH, 1).leg(0, 100, 100).arms(-90, -90)
                    .label("ONE LEG UP").move(1.1).hold(0.3)
                    .next().t(-90).sh(-0.47, 0.47).h(-20).footTo(1, 0.45, AH, 1).leg(0, 90, 90).arms(-90, -90)
                    .label("DRIVE THROUGH THE HEEL").hold(0.9).add();
                return a.done();
            }
            case "chair_squat": {
                A a = new A(SIDE, GLUTE | THIGH).prop(CHAIR, -0.75, -0.30, 0.46);
                a.k().at(0.04, 0.94).t(178).feetTo(0.08, AH).arms(4, 10).label("STAND TALL").move(1.2).hold(0.3)
                    .next().at(-0.33, 0.55).t(145).h(25).feetTo(0.08, AH).arms(95, 95).label("SIT BACK, TAP THE SEAT").hold(0.3)
                    .next().at(0.04, 0.94).t(178).h(0).feetTo(0.08, AH).arms(4, 10).label("STAND UP, SQUEEZE").hold(0.6).add();
                return a.done();
            }
            case "squat": {
                A a = new A(SIDE, GLUTE | THIGH);
                a.k().at(0.02, 0.94).t(178).feetTo(0.08, AH).arms(4, 10).label("FEET SHOULDER-WIDTH").move(1.2).hold(0.3)
                    .next().at(-0.30, 0.50).t(142).h(28).feetTo(0.08, AH).arms(95, 95).label("HIPS BACK AND DOWN, CHEST UP").hold(0.3)
                    .next().at(0.02, 0.94).t(178).h(0).feetTo(0.08, AH).arms(4, 10).label("STAND, SQUEEZE").hold(0.6).add();
                return a.done();
            }
            case "reverse_lunge": {
                A a = new A(SIDE, GLUTE | THIGH);
                a.k().at(0.03, STAND - 0.01).footTo(0, 0.06, AH, 1).footTo(1, 0.04, AH, 1).handsOnHips().label("STAND TALL").move(1.2).hold(0.3)
                    .next().at(-0.18, 0.52).footTo(0, 0.06, AH, 1).footTo(1, -0.62, 0.14, 1).foot(1, 40).handsOnHips()
                    .label("STEP BACK, BOTH KNEES BEND").hold(0.4)
                    .next().at(0.03, STAND - 0.01).footTo(0, 0.06, AH, 1).footTo(1, 0.04, AH, 1).foot(1, Double.NaN).handsOnHips()
                    .label("PUSH THROUGH THE FRONT HEEL").hold(0.4).add();
                return a.done();
            }
            case "split_squat": {
                A a = new A(SIDE, GLUTE | THIGH).prop(COUCH, -1.5, -0.62, 0.42);
                a.k().at(-0.12, 0.84).t(172).footTo(0, 0.20, AH, 1).footTo(1, -0.68, 0.48, 1).foot(1, -100).handsOnHips()
                    .label("BACK FOOT ON THE COUCH").move(1.2).hold(0.3)
                    .next().at(-0.18, 0.52).t(165).footTo(0, 0.20, AH, 1).footTo(1, -0.68, 0.48, 1).foot(1, -100).handsOnHips()
                    .label("LOWER STRAIGHT DOWN").hold(0.4).add();
                return a.done();
            }
            case "sumo_squat": {
                A a = new A(FRONT, GLUTE | INNER | THIGH);
                a.k().at(0, 0.91).footTo(0, 0.36, AH, 1).footTo(1, -0.36, AH, -1).handsTo(0, 1.20, 1)
                    .label("WIDE STANCE, TOES OUT").move(1.2).hold(0.3)
                    .next().at(0, 0.60).footTo(0, 0.36, AH, 1).footTo(1, -0.36, AH, -1).handsTo(0, 0.90, 1)
                    .label("KNEES PUSH OUT").hold(0.4).add();
                return a.done();
            }
            case "curtsy_lunge": {
                A a = new A(FRONT, GLUTE | HIPSIDE | THIGH);
                a.k().at(0, STAND - 0.02).footTo(0, 0.13, AH, 1).footTo(1, -0.13, AH, -1).handsTo(0, 1.20, 1)
                    .label("STAND TALL").move(1.2).hold(0.3)
                    .next().at(-0.08, 0.64).behind().footTo(0, -0.36, 0.16, 1).footTo(1, -0.12, AH, -1).handsTo(-0.08, 0.93, 1)
                    .label("STEP BEHIND AND ACROSS, LOWER").hold(0.4).add();
                return a.done();
            }
            case "step_up": {
                A a = new A(SIDE, GLUTE | THIGH).prop(STEP, 0.05, 0.9, 0.20);
                a.k().at(-0.28, STAND).footTo(0, -0.26, AH, 1).footTo(1, -0.30, AH, 1).label("STAND IN FRONT OF THE STEP").move(1.0).hold(0.2)
                    .next().at(-0.24, STAND - 0.02).footTo(0, 0.20, 0.28, 1).footTo(1, -0.30, AH, 1).label("ONE FOOT UP").hold(0.2)
                    .next().at(0.18, STAND + 0.18).footTo(0, 0.20, 0.28, 1).footTo(1, 0.14, 0.36, 1)
                    .label("PUSH THROUGH THAT HEEL, STAND TALL").hold(0.5)
                    .next().at(-0.24, STAND - 0.02).footTo(0, 0.20, 0.28, 1).footTo(1, -0.30, AH, 1).label("STEP DOWN SLOWLY").hold(0.2).add();
                return a.done();
            }
            case "calf_raise": {
                A a = new A(SIDE, SHIN).prop(WALL, 0.48, 0.48, 0);
                a.k().at(0, STAND).feetTo(0, AH).handsTo(0.46, 1.22, -1).label("HANDS ON THE WALL").move(0.9).hold(0.3)
                    .next().at(0, STAND + 0.09).feetTo(0, AH + 0.09).foot(0, 45).foot(1, 45).handsTo(0.46, 1.22, -1)
                    .label("RISE ONTO YOUR TOES, PAUSE").hold(0.7).add();
                return a.done();
            }
            case "clamshell": {
                A a = new A(FRONT, HIPSIDE | GLUTE).noFloor();
                B b = a.k().at(0, 0.21).t(-90).arm(1, -90, -90).arm(0, 0, 0).leg(1, 55, -55).leg(0, 57, -57)
                    .label("ON YOUR SIDE, KNEES BENT (FROM THE FRONT)").move(1.0).hold(0.3);
                J j = b.now();
                double fx = j.anF[0], fy = j.anF[1] + 0.05;
                b.next().thighFoot(0, 110, fx, fy).label("FEET TOGETHER, OPEN THE TOP KNEE").hold(0.6).add();
                return a.done();
            }
            case "side_leg_raise": {
                A a = new A(FRONT, HIPSIDE | GLUTE).prop(MAT, -1.0, 1.2, 0);
                a.k().at(0, 0.21).t(-90).arm(1, -90, -90).arm(0, 0, 0).leg(1, 90, 90).leg(0, 92, 92)
                    .label("LIE ON YOUR SIDE, LEGS STRAIGHT").move(1.0).hold(0.3)
                    .next().leg(0, 125, 125).label("LIFT THE TOP LEG, SLOWLY").hold(0.6).add();
                return a.done();
            }
            case "fire_hydrant": {
                A a = new A(FRONT, HIPSIDE | GLUTE).noFloor();
                a.k().at(0, 0.5).t(90).h(0).legScale(0, 0.18, 1).legScale(1, 0.18, 1).leg(0, 0, -90).leg(1, 180, -90)
                    .foot(0, -110).foot(1, -70).armScale(0, 0.2, 0.2).armScale(1, 0.2, 0.2).arm(0, 0, 0).arm(1, 180, 180)
                    .label("HANDS AND KNEES (SEEN FROM ABOVE)").move(1.0).hold(0.3)
                    .next().legScale(0, 1, 1).leg(0, 0, -90).label("LIFT THE KNEE OUT TO THE SIDE").hold(0.6).add();
                return a.done();
            }
            case "standing_abduction": {
                A a = new A(FRONT, HIPSIDE | GLUTE);
                a.k().footTo(0, 0.12, AH, 1).footTo(1, -0.12, AH, -1).handsOnHips().label("STAND TALL").move(1.0).hold(0.3)
                    .next().at(-0.03, STAND).t(182).leg(0, 32, 32).footTo(1, -0.12, AH, -1).handsOnHips()
                    .label("LIFT ONE LEG OUT, DON'T LEAN").hold(0.6).add();
                return a.done();
            }
            case "donkey_kick": {
                A a = new A(SIDE, GLUTE).prop(MAT, -1.0, 0.8, 0);
                a.k().at(0, 0.50).t(85).h(-10).kneeTo(0, 0.0, 0.06).kneeTo(1, 0.0, 0.06).shin(0, -90).shin(1, -90).foot(0, -170).foot(1, -170)
                    .handsTo(0.50, 0.04, 1).label("HANDS AND KNEES").move(1.0).hold(0.3)
                    .next().leg(0, -95, 165).foot(0, 75).handsTo(0.50, 0.04, 1).label("PRESS THE FOOT UP, SQUEEZE").hold(0.6).add();
                return a.done();
            }
            case "frog_pump": {
                A a = new A(SIDE, GLUTE).prop(MAT, -0.9, 0.7, 0);
                a.k().at(0, 0.10).t(-90).footTo(0, 0.28, AH, 1).footTo(1, 0.26, AH, 1).arms(90, 90)
                    .label("SOLES TOGETHER, KNEES FALL OPEN").move(0.8).hold(0.2)
                    .next().t(-68).sh(-0.47, 0.10).h(-22).footTo(0, 0.28, AH, 1).footTo(1, 0.26, AH, 1).arms(90, 90)
                    .label("SQUEEZE HARD, LIFT").hold(0.5).add();
                return a.done();
            }
            case "glute_kickback": {
                A a = new A(SIDE, GLUTE).prop(WALL, 0.55, 0.55, 0);
                a.k().at(0, STAND).t(172).feetTo(0.02, AH).handsTo(0.53, 1.20, -1).label("HANDS ON THE WALL").move(0.9).hold(0.2)
                    .next().leg(0, -28, -24).footTo(1, 0.02, AH, 1).handsTo(0.53, 1.20, -1).label("KICK BACK, SQUEEZE").hold(0.6).add();
                return a.done();
            }

            // ===== STRENGTH =====
            case "wall_pushup": {
                A a = new A(SIDE, CHEST | ARM).prop(WALL, 0.66, 0.66, 0);
                a.k().line(-0.12, AH, 160).handsTo(0.64, 1.30, 1).label("HANDS ON THE WALL, BODY STRAIGHT").move(1.2).hold(0.3)
                    .next().line(-0.12, AH, 150).handsTo(0.64, 1.30, 1).label("CHEST TO THE WALL").hold(0.3).add();
                return a.done();
            }
            case "counter_pushup": {
                A a = new A(SIDE, CHEST | ARM).prop(COUNTER, 0.86, 1.5, 0.90);
                a.k().line(-0.42, AH, 128).handsTo(0.88, 0.92, 1).label("HANDS ON THE COUNTER EDGE").move(1.2).hold(0.3)
                    .next().line(-0.42, AH, 117).handsTo(0.88, 0.92, 1).label("LOWER YOUR CHEST TO THE EDGE").hold(0.3).add();
                return a.done();
            }
            case "knee_pushup": {
                A a = new A(SIDE, CHEST | ARM).prop(MAT, -0.7, 0.9, 0);
                a.k().lineKnee(-0.04, 0.06, 116, -150).foot(0, -100).foot(1, -100).handsTo(0.58, 0.04, 1)
                    .label("KNEES DOWN, BODY STRAIGHT").move(1.2).hold(0.3)
                    .next().lineKnee(-0.04, 0.06, 99, -150).foot(0, -100).foot(1, -100).handsTo(0.58, 0.04, 1)
                    .label("LOWER YOUR CHEST").hold(0.3).add();
                return a.done();
            }
            case "pushup": {
                A a = new A(SIDE, CHEST | ARM).prop(MAT, -1.2, 0.9, 0);
                a.k().line(-0.86, 0.06, 111).foot(0, 160).foot(1, 160).handsTo(0.44, 0.04, 1)
                    .label("STRAIGHT LINE, HEAD TO HEELS").move(1.2).hold(0.3)
                    .next().line(-0.86, 0.06, 96).foot(0, 160).foot(1, 160).handsTo(0.44, 0.04, 1)
                    .label("CHEST A FIST OFF THE FLOOR").hold(0.3).add();
                return a.done();
            }
            case "decline_pushup": {
                A a = new A(SIDE, CHEST | ARM | SHOULDER).prop(COUCH, -1.9, -0.82, 0.42);
                a.k().line(-0.88, 0.46, 97).foot(0, 160).foot(1, 160).handsTo(0.48, 0.04, 1)
                    .label("FEET ON THE COUCH").move(1.2).hold(0.3)
                    .next().line(-0.88, 0.46, 80).foot(0, 160).foot(1, 160).handsTo(0.48, 0.04, 1)
                    .label("LOWER WITH CONTROL").hold(0.3).add();
                return a.done();
            }
            case "wall_angel": {
                A a = new A(FRONT, SHOULDER | BACK).prop(WALL, 0, 0, 0);
                a.k().footTo(0, 0.12, AH, 1).footTo(1, -0.12, AH, -1).arm(0, 90, 180).arm(1, -90, -180)
                    .label("BACK FLAT ON THE WALL, ARMS LIKE A GOALPOST").move(1.4).hold(0.2)
                    .next().arm(0, 150, 175).arm(1, -150, -175).label("SLIDE UP, KEEP CONTACT").hold(0.4).add();
                return a.done();
            }
            case "prone_ytw": {
                A a = new A(SIDE, BACK | SHOULDER).prop(MAT, -1.0, 1.1, 0).mirror();
                a.k().at(0, 0.10).t(90).h(10).legs(-90, -90).foot(0, 180).foot(1, 180).arms(104, 104)
                    .label("Y: ARMS FORWARD AND UP, THUMBS UP").move(1.0).hold(0.5)
                    .next().armScale(0, 0.35, 0.3).armScale(1, 0.35, 0.3).arms(180, 180).label("T: ARMS OUT TO THE SIDES, LIFT").hold(0.5)
                    .next().armScale(0, 1, 1).armScale(1, 1, 1).arms(-145, 150).label("W: ELBOWS BACK, SQUEEZE YOUR SHOULDER BLADES").hold(0.5).add();
                return a.done();
            }
            case "superman": {
                A a = new A(SIDE, BACK | GLUTE).prop(MAT, -1.0, 1.1, 0).mirror();
                a.k().at(0, 0.10).t(90).legs(-90, -90).foot(0, 180).foot(1, 180).arms(90, 90).label("FACE DOWN, ARMS FORWARD").move(1.2).hold(0.3)
                    .next().at(0, 0.10).t(80).bend(-0.04).h(5).legs(-80, -78).foot(0, 180).foot(1, 180).arms(105, 108)
                    .label("LIFT ARMS, CHEST AND LEGS, HOLD").hold(1.6).add();
                return a.done();
            }
            case "chair_dip_bent": {
                A a = new A(SIDE, ARM).prop(CHAIR, -0.72, -0.27, 0.46).mirror();
                a.k().t(178).at(-0.20, 0.42).footTo(0, 0.38, AH, 1).footTo(1, 0.36, AH, 1).handsTo(-0.27, 0.48, -1)
                    .label("HANDS ON THE SEAT EDGE BEHIND YOU").move(1.2).hold(0.3)
                    .next().at(-0.20, 0.22).footTo(0, 0.38, AH, 1).footTo(1, 0.36, AH, 1).handsTo(-0.27, 0.48, -1)
                    .label("BEND YOUR ELBOWS STRAIGHT BACK").hold(0.3).add();
                return a.done();
            }
            case "chair_dip": {
                A a = new A(SIDE, ARM).prop(CHAIR, -0.72, -0.27, 0.46).mirror();
                a.k().t(178).at(-0.18, 0.42).footTo(0, 0.62, AH, 1).footTo(1, 0.60, AH, 1).handsTo(-0.27, 0.48, -1)
                    .label("LEGS LONGER").move(1.2).hold(0.3)
                    .next().at(-0.18, 0.20).footTo(0, 0.62, AH, 1).footTo(1, 0.60, AH, 1).handsTo(-0.27, 0.48, -1)
                    .label("LOWER, SHOULDERS DOWN").hold(0.3).add();
                return a.done();
            }

            // ===== CORE =====
            case "wall_plank": {
                A a = new A(SIDE, CORE).prop(WALL, 0.70, 0.70, 0);
                a.k().line(-0.26, AH, 152).arm(0, 60, 90).arm(1, 58, 90).label("FOREARMS ON THE WALL").move(1.0).hold(0.4)
                    .next().label("BRACE, SQUEEZE GLUTES, BREATHE").hold(2.0).add();
                Anim an = a.done();
                for (Pose k : an.keys) { // elbow against the wall, forearm flat up the wall
                    J j = joints(k, SIDE);
                    double dx = 0.70 - 0.05 - j.sh[0];
                    k.arm[0] = k.arm[2] = dx < UA ? angleOf(dx, -Math.sqrt(UA * UA - dx * dx)) : 90;
                    k.arm[1] = k.arm[3] = 180;
                }
                return breathe(an);
            }
            case "counter_plank": {
                A a = new A(SIDE, CORE).prop(COUNTER, 0.80, 1.5, 0.90);
                a.k().line(-0.52, AH, 120).label("FOREARMS ON THE COUNTER").move(1.0).hold(0.4)
                    .next().label("STRAIGHT LINE, BREATHE").hold(2.0).add();
                Anim an = a.done();
                for (Pose k : an.keys) { k.arm[0] = k.arm[2] = 0; k.arm[1] = k.arm[3] = 90; }
                return breathe(an);
            }
            case "knee_plank": {
                A a = new A(SIDE, CORE).prop(MAT, -0.7, 0.9, 0);
                a.k().lineKnee(-0.04, 0.06, 101, -150).foot(0, -100).foot(1, -100).arm(0, 0, 90).arm(1, -2, 90)
                    .label("FOREARMS AND KNEES").move(1.0).hold(0.4).next().label("HIPS LEVEL, DON'T SAG").hold(2.0).add();
                return breathe(a.done());
            }
            case "plank": {
                A a = new A(SIDE, CORE).prop(MAT, -1.1, 0.8, 0);
                a.k().line(-0.86, 0.06, 98).foot(0, 160).foot(1, 160).arm(0, 0, 90).arm(1, -2, 90)
                    .label("FOREARMS AND TOES").move(1.0).hold(0.4).next().label("ONE STRAIGHT LINE, BREATHE").hold(2.0).add();
                return breathe(a.done());
            }
            case "plank_taps": {
                A a = new A(SIDE, CORE | SHOULDER).prop(MAT, -1.2, 0.9, 0);
                a.k().line(-0.86, 0.06, 111).foot(0, 160).foot(1, 160).handsTo(0.44, 0.04, 1)
                    .label("HIGH PLANK, FEET WIDE").move(0.6).hold(0.2)
                    .next().arm(0, 150, 40).handTo(1, 0.44, 0.04, 1).label("TAP A SHOULDER, HIPS STILL").hold(0.3)
                    .next().handsTo(0.44, 0.04, 1).label("HAND BACK DOWN").hold(0.2)
                    .next().arm(1, 150, 40).handTo(0, 0.44, 0.04, 1).label("OTHER SIDE").hold(0.3).add();
                return a.done();
            }
            case "knee_side_plank": {
                A a = new A(FRONT, CORE | HIPSIDE).noFloor();
                a.k().at(0, 0.20).t(-86).leg(1, 125, 125).leg(0, 123, 123).legScale(0, 1, 0.3).legScale(1, 1, 0.3).arm(1, 0, 0).armScale(1, 1, 0.3).arm(0, 92, 90)
                    .ground().label("ON YOUR SIDE, ELBOW DOWN (FROM THE FRONT)").move(1.0).hold(0.3)
                    .next().t(-68).leg(1, 140, 140).leg(0, 138, 138).arm(1, 0, 0).arm(0, 180, 180).ground()
                    .label("LIFT YOUR HIPS, HOLD").hold(2.0).add();
                return a.done();
            }
            case "side_plank": {
                A a = new A(FRONT, CORE | HIPSIDE).noFloor();
                a.k().at(0, 0.20).t(-86).legs(94, 94).leg(0, 96, 96).arm(1, 0, 0).armScale(1, 1, 0.3).arm(0, 92, 90)
                    .ground().label("ON YOUR SIDE, ELBOW DOWN (FROM THE FRONT)").move(1.0).hold(0.3)
                    .next().t(-72).legs(108, 108).leg(0, 110, 110).arm(1, 0, 0).arm(0, 180, 180).ground()
                    .label("LIFT INTO A STRAIGHT LINE, HOLD").hold(2.0).add();
                return a.done();
            }
            case "dead_bug": {
                A a = new A(SIDE, CORE).prop(MAT, -1.0, 1.0, 0);
                a.k().at(0, 0.10).t(-90).leg(0, 180, 90).leg(1, 178, 90).arms(180, 180).label("ARMS UP, KNEES OVER HIPS").move(1.0).hold(0.3)
                    .next().leg(0, 105, 100).arm(1, 250, 260).label("OPPOSITE ARM AND LEG REACH, BACK FLAT").hold(0.4)
                    .next().leg(0, 180, 90).arms(180, 180).label("BACK TO THE MIDDLE").hold(0.2)
                    .next().leg(1, 105, 100).arm(0, 250, 260).label("OTHER SIDE").hold(0.4).add();
                return a.done();
            }
            case "bird_dog": {
                A a = new A(SIDE, CORE | GLUTE | BACK).prop(MAT, -1.0, 1.2, 0);
                a.k().at(0, 0.50).t(85).h(-10).kneeTo(0, 0.0, 0.06).kneeTo(1, 0.0, 0.06).shin(0, -90).shin(1, -90)
                    .foot(0, -170).foot(1, -170).handsTo(0.50, 0.04, 1).label("HANDS AND KNEES").move(1.0).hold(0.3)
                    .next().leg(1, -88, -88).foot(1, 180).arm(0, 97, 97).handTo(1, 0.50, 0.04, 1)
                    .label("REACH ONE ARM AND THE OPPOSITE LEG").hold(0.8).add();
                return a.done();
            }
            case "reverse_crunch": {
                A a = new A(SIDE, CORE).prop(MAT, -1.0, 0.9, 0);
                a.k().at(0, 0.10).t(-90).leg(0, 150, 60).leg(1, 148, 60).arms(90, 90).label("KNEES BENT, FEET UP").move(1.0).hold(0.2)
                    .next().t(-68).sh(-0.47, 0.10).h(-22).leg(0, 205, 120).leg(1, 203, 120).arms(90, 90)
                    .label("CURL YOUR HIPS UP").hold(0.4).add();
                return a.done();
            }
            case "vacuum": {
                A a = new A(SIDE, CORE);
                a.k().feetTo(0.02, AH).bend(0).label("BREATHE ALL THE WAY OUT").move(1.5).hold(0.4)
                    .next().bend(0.05).label("PULL YOUR BELLY BUTTON IN, HOLD").hold(2.5).add();
                return a.done();
            }

            // ===== BALANCE =====
            case "tandem_stance": {
                A a = new A(SIDE, SHIN | HIPSIDE).prop(WALL, -0.45, -0.45, 0);
                a.k().footTo(0, 0.12, AH, 1).footTo(1, -0.06, AH, 1).arm(0, 30, 40).arm(1, -20, -10)
                    .label("HEEL TO TOE").move(1.2).hold(0.4).next().arm(0, 36, 44).arm(1, -26, -14).at(0.01, STAND)
                    .label("STAND TALL, EYES AHEAD").hold(2.0).add();
                return a.done();
            }
            case "single_leg_stand": {
                A a = new A(SIDE, SHIN | HIPSIDE).prop(WALL, -0.45, -0.45, 0);
                a.k().feetTo(0.02, AH).label("NEAR A WALL").move(1.0).hold(0.3)
                    .next().footTo(1, 0.02, AH, 1).leg(0, 60, -10).arm(0, 40, 50).arm(1, -30, -20).label("LIFT ONE FOOT, HOLD").hold(2.0).add();
                return a.done();
            }
            case "sl_stand_closed": {
                A a = new A(SIDE, SHIN | HIPSIDE).prop(WALL, -0.45, -0.45, 0);
                a.k().feetTo(0.02, AH).label("RIGHT NEXT TO A WALL").move(1.0).hold(0.3)
                    .next().footTo(1, 0.02, AH, 1).leg(0, 60, -10).arm(0, 40, 50).arm(1, -30, -20).eyesClosed()
                    .label("ONE FOOT UP, CLOSE YOUR EYES").hold(2.0).add();
                return a.done();
            }

            // ===== STAMINA =====
            case "march": case "dance_unused": {
                A a = new A(SIDE, HEART);
                a.k().footTo(1, 0.0, AH, 1).leg(0, 75, -10).arm(0, -30, 10).arm(1, 40, 80).label("KNEE UP, OPPOSITE ARM").move(0.4).hold(0)
                    .next().feetTo(0.0, AH).arm(0, 4, 10).arm(1, -4, 4).label("KEEP MARCHING").hold(0)
                    .next().footTo(0, 0.0, AH, 1).leg(1, 75, -10).arm(1, -30, 10).arm(0, 40, 80).label("SWING YOUR ARMS").hold(0)
                    .next().feetTo(0.0, AH).arm(0, 4, 10).arm(1, -4, 4).label("YOU CAN STILL TALK").hold(0).add();
                return a.done();
            }
            case "dance": {
                A a = new A(FRONT, HEART | HIPSIDE);
                a.k().at(-0.06, STAND - 0.04).t(186).footTo(0, 0.16, AH, 1).footTo(1, -0.16, AH, -1).arm(0, 130, 170).arm(1, -40, -100)
                    .label("PUT ON A SONG YOU LOVE").move(0.45).hold(0)
                    .next().at(0.06, STAND - 0.06).t(174).footTo(0, 0.16, AH, 1).footTo(1, -0.16, AH, -1).arm(0, 40, 100).arm(1, -130, -170)
                    .label("ANY MOVES COUNT").hold(0)
                    .next().at(0, STAND - 0.12).t(180).footTo(0, 0.20, AH, 1).footTo(1, -0.20, AH, -1).arm(0, 60, 150).arm(1, -60, -150)
                    .label("KEEP MOVING").hold(0).add();
                return a.done();
            }
            case "step_jacks": {
                A a = new A(FRONT, HEART);
                a.k().footTo(0, 0.10, AH, 1).footTo(1, -0.10, AH, -1).label("FEET TOGETHER").move(0.5).hold(0.05)
                    .next().at(-0.04, STAND - 0.01).footTo(0, 0.38, AH, 1).footTo(1, -0.10, AH, -1).arm(0, 150, 170).arm(1, -150, -170)
                    .label("STEP OUT, ARMS UP").hold(0.05)
                    .next().at(0, STAND).footTo(0, 0.10, AH, 1).footTo(1, -0.10, AH, -1).arm(0, 12, 8).arm(1, -12, -8)
                    .label("BACK IN").hold(0.05)
                    .next().at(0.04, STAND - 0.01).footTo(0, 0.10, AH, 1).footTo(1, -0.38, AH, -1).arm(0, 150, 170).arm(1, -150, -170)
                    .label("OTHER SIDE").hold(0.05).add();
                return a.done();
            }
            case "stair_climb": {
                A a = new A(SIDE, HEART | GLUTE).prop(STAIRS, 0.0, 1.5, 0.18);
                a.k().at(-0.10, STAND + 0.10).footTo(0, 0.14, 0.26, 1).footTo(1, -0.10, AH + 0.0, 1).arm(0, -25, -10).arm(1, 30, 50)
                    .label("UP THE STAIRS, HOLD THE RAIL").move(0.6).hold(0)
                    .next().at(0.16, STAND + 0.26).footTo(0, 0.14, 0.26, 1).footTo(1, 0.40, 0.44, 1).arm(1, -25, -10).arm(0, 30, 50)
                    .label("STEADY PACE").hold(0).add();
                return a.done();
            }
            case "high_knees": {
                A a = new A(SIDE, HEART);
                a.k().at(0, STAND + 0.03).footTo(1, 0.0, AH + 0.04, 1).foot(1, 60).leg(0, 100, 0).arm(0, -40, 60).arm(1, 60, 150)
                    .label("KNEES UP HIGH").move(0.25).hold(0)
                    .next().at(0, STAND + 0.03).footTo(0, 0.0, AH + 0.04, 1).foot(0, 60).foot(1, Double.NaN).leg(1, 100, 0).arm(1, -40, 60).arm(0, 60, 150)
                    .label("LIGHT ON YOUR FEET").hold(0).add();
                return a.done();
            }

            // ===== the moves his arc adds =====
            case "door_row": {
                A a = new A(SIDE, BACK | ARM).prop(DOOR, 0.46, 0.46, 0);
                a.k().line(0.20, AH, 203).handsTo(0.44, 1.12, 1).label("GRIP THE FRAME, LEAN BACK, ARMS STRAIGHT").move(1.1).hold(0.3)
                    .next().line(0.20, AH, 190).handsTo(0.44, 1.12, 1).label("PULL YOUR CHEST TO THE FRAME, SQUEEZE").hold(0.5).add();
                return a.done();
            }
            case "table_row": {
                A a = new A(SIDE, BACK | ARM).prop(TABLE, -0.80, 0.05, 0.72);
                a.k().line(0.90, 0.06, -93).foot(0, 180).foot(1, 180).handsTo(-0.47, 0.70, -1)
                    .label("UNDER A STURDY TABLE, ARMS STRAIGHT").move(1.1).hold(0.3)
                    .next().line(0.90, 0.06, -110).foot(0, 180).foot(1, 180).handsTo(-0.40, 0.70, -1)
                    .label("PULL YOUR CHEST UP TO THE TABLE").hold(0.5).add();
                return a.done();
            }
            case "diamond_pushup": {
                A a = new A(SIDE, ARM | CHEST).prop(MAT, -1.2, 0.9, 0);
                a.k().line(-0.86, 0.06, 111).foot(0, 160).foot(1, 160).handsTo(0.40, 0.04, 1)
                    .label("HANDS TOGETHER UNDER YOUR CHEST").move(1.2).hold(0.3)
                    .next().line(-0.86, 0.06, 97).foot(0, 160).foot(1, 160).handsTo(0.40, 0.04, 1)
                    .label("ELBOWS BRUSH YOUR SIDES").hold(0.3).add();
                return a.done();
            }
            case "pike_pushup": {
                A a = new A(SIDE, SHOULDER | ARM).prop(MAT, -0.9, 0.9, 0);
                a.k().t(50).sh(0.38, 0.56).h(-10).feetTo(-0.55, 0.10).foot(0, 70).foot(1, 70).handsTo(0.46, 0.04, 1)
                    .label("HIPS HIGH, AN UPSIDE-DOWN V").move(1.2).hold(0.3)
                    .next().t(38).sh(0.42, 0.30).h(-10).feetTo(-0.55, 0.10).foot(0, 70).foot(1, 70).handsTo(0.46, 0.04, 1)
                    .label("LOWER YOUR HEAD BETWEEN YOUR HANDS").hold(0.3).add();
                return a.done();
            }
            case "shadow_box": {
                A a = new A(SIDE, HEART | SHOULDER);
                B b = a.k().at(0, STAND - 0.05).t(176).footTo(0, 0.22, AH, 1).footTo(1, -0.20, AH, 1).foot(1, 60)
                    .handsTo(0.16, 1.40, 1).label("HANDS UP BY YOUR CHIN").move(0.25).hold(0.1);
                b = b.next().arm(0, 92, 90).label("JAB").hold(0.05);
                b = b.next().handsTo(0.16, 1.40, 1).label("BACK TO GUARD").hold(0.08);
                b.next().arm(1, 94, 90).handTo(0, 0.16, 1.40, 1).label("CROSS").hold(0.05).add();
                return a.done();
            }
            case "mountain_climber": {
                A a = new A(SIDE, HEART | CORE).prop(MAT, -1.2, 0.9, 0);
                a.k().line(-0.86, 0.06, 111).foot(0, 160).foot(1, 160).handsTo(0.44, 0.04, 1)
                    .footTo(0, 0.02, 0.14, -1).label("HIGH PLANK, KNEE TO CHEST").move(0.3).hold(0)
                    .next().line(-0.86, 0.06, 111).foot(0, 160).foot(1, 160).handsTo(0.44, 0.04, 1)
                    .footTo(1, 0.02, 0.14, -1).label("SWITCH FAST, HIPS DOWN").hold(0).add();
                return a.done();
            }
            case "burpee": {
                A a = new A(SIDE, HEART | THIGH).prop(MAT, -1.2, 0.9, 0);
                a.k().feetTo(0.02, AH).label("STAND").move(0.5).hold(0.1)
                    .next().at(-0.18, 0.50).t(120).h(20).feetTo(0.02, AH).handsTo(0.36, 0.04, 1).label("SQUAT, HANDS DOWN").hold(0.05)
                    .next().line(-0.86, 0.06, 111).h(0).foot(0, 160).foot(1, 160).handsTo(0.44, 0.04, 1).label("FEET BACK TO A PLANK").hold(0.2)
                    .next().at(-0.18, 0.50).t(120).h(20).feetTo(0.02, AH).foot(0, Double.NaN).foot(1, Double.NaN).handsTo(0.36, 0.04, 1)
                    .label("FEET BACK IN").hold(0.05)
                    .next().at(0, STAND + 0.06).t(180).h(0).feetTo(0.02, AH + 0.06).foot(0, 40).foot(1, 40).arms(178, 180)
                    .label("STAND AND REACH UP").hold(0.2).add();
                return a.done();
            }
            case "jump_squat": {
                A a = new A(SIDE, GLUTE | THIGH);
                a.k().at(-0.28, 0.52).t(142).h(28).feetTo(0.08, AH).arms(-30, -20).label("SQUAT DOWN").move(0.5).hold(0.15)
                    .next().at(0.04, STAND + 0.16).t(178).h(0).feetTo(0.06, AH + 0.16).foot(0, 40).foot(1, 40).arms(170, 175)
                    .label("JUMP STRAIGHT UP").hold(0.05)
                    .next().at(-0.28, 0.52).t(142).h(28).feetTo(0.08, AH).foot(0, Double.NaN).foot(1, Double.NaN).arms(-30, -20)
                    .label("LAND SOFTLY, INTO THE NEXT ONE").hold(0.15).add();
                return a.done();
            }

            // ===== test-only =====
            case "sit_rise": {
                A a = new A(SIDE, THIGH | GLUTE).prop(MAT, -0.8, 0.9, 0);
                a.k().feetTo(0.02, AH).arms(80, 85).label("STAND, ARMS FORWARD").move(1.2).hold(0.3)
                    .next().at(-0.08, 0.55).t(160).feetTo(0.12, AH).arms(90, 90).label("CROSS YOUR LEGS AND LOWER").hold(0.2)
                    .next().at(-0.06, 0.12).t(174).footTo(0, 0.24, 0.06, 1).footTo(1, 0.28, 0.06, 1).arms(80, 85)
                    .label("SIT, NO HANDS IF YOU CAN").hold(0.6)
                    .next().at(-0.08, 0.55).t(160).feetTo(0.12, AH).arms(90, 90).label("STAND BACK UP").hold(0.2)
                    .next().at(0, STAND).t(180).feetTo(0.02, AH).arms(80, 85).label("-1 POINT FOR EACH HAND OR KNEE YOU USE").hold(0.8).add();
                return a.done();
            }
            case "jog": {
                A a = new A(SIDE, HEART);
                a.k().at(0, STAND - 0.02).t(172).footTo(1, -0.18, AH, 1).leg(0, 55, -30).arm(0, -40, 50).arm(1, 40, 120)
                    .label("STEADY JOG").move(0.32).hold(0)
                    .next().at(0, STAND - 0.02).t(172).footTo(0, -0.18, AH, 1).leg(1, 55, -30).arm(1, -40, 50).arm(0, 40, 120)
                    .label("12 MINUTES, AS FAR AS YOU CAN").hold(0).add();
                return a.done();
            }
            default: {
                A a = new A(SIDE, 0);
                a.k().feetTo(0.02, AH).add();
                return a.done();
            }
        }
    }

    /** Holds get a gentle breathing sway so they never look frozen. */
    static Anim breathe(Anim a) {
        Pose last = a.keys.get(a.keys.size() - 1);
        Pose b = last.copy();
        b.bend += 0.012;
        b.label = last.label;
        b.move = 1.0;
        b.hold = 0.2;
        last.hold = 0.2;
        a.keys.add(b);
        return a;
    }

    /** Thigh angle from a hip to a knee point (for kneeling poses where only the knee matters). */
    static double a2(double hx, double hy, double kx, double ky) {
        return angleOf(kx - hx, ky - hy);
    }

}
