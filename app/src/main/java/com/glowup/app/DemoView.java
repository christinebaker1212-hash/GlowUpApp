package com.glowup.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;

/** Plays an exercise's demonstration on a loop, with the working muscles lit up and its cue words underneath. */
final class DemoView extends View {
    static final boolean M = Flavor.MALE;
    static final int BODY = M ? 0xFFE3EAF7 : 0xFFEDE3FA, FAR = M ? 0xFF7D8BA6 : 0xFF8E7FA6,
        HAIR = M ? 0xFF8A5A3C : 0xFFFFB45E, HOT = Ui.PINK, HOT_FAR = M ? 0xFFB3550F : 0xFFB0397A,
        PROP = M ? 0xFF243049 : 0xFF3A2C4E, PROP_EDGE = M ? 0xFF3D4F73 : 0xFF5E4A7A, FLOOR = M ? 0xFF3D4F73 : 0xFF5E4A7A,
        MAT = M ? 0xFF16203A : 0xFF2E2042, INK = 0xFF2A1F38, OUTLINE = M ? 0xFF0E1420 : 0xFF1A1324;

    final Demo.Anim anim;
    final int wantH;
    final long start = SystemClock.uptimeMillis();
    boolean showLabel = true;
    double speed = 1;

    DemoView(Context c, Demo.Anim anim, int heightDp) {
        super(c);
        this.anim = anim;
        this.wantH = Ui.dp(c, heightDp);
    }

    @Override
    protected void onMeasure(int ws, int hs) {
        setMeasuredDimension(MeasureSpec.getSize(ws), wantH);
    }

    @Override
    protected void onDraw(Canvas cv) {
        double s = (SystemClock.uptimeMillis() - start) / 1000.0 * speed;
        Demo.Pose pose = Demo.at(anim, s);
        draw(cv, anim, pose, getWidth(), getHeight(), getResources().getDisplayMetrics().density, showLabel);
        if (isShown()) postInvalidateOnAnimation();
    }

    // ---------------- drawing (also used by the tests to render keyframes) ----------------

    static double[] bounds(Demo.Anim a) {
        double[] b = {1e9, 0, -1e9, -1e9};
        for (Demo.Pose k : a.keys) {
            Demo.J j = Demo.joints(k, a.view);
            for (double[] q : new double[][]{j.hip, j.hipN, j.hipF, j.sh, j.shN, j.shF, j.elN, j.waN, j.elF, j.waF,
                j.knN, j.anN, j.toN, j.knF, j.anF, j.toF}) grow(b, q, 0.10);
            grow(b, j.head, Demo.HEAD + 0.08);
        }
        for (double[] pr : a.props) {
            int type = (int) pr[0];
            if (type == Demo.WALL) continue;
            if (type == Demo.DOOR) { grow(b, new double[]{pr[1] + 0.1, 0}, 0.02); continue; }
            grow(b, new double[]{pr[1], pr[3]}, 0.02);
            grow(b, new double[]{pr[2], pr[3]}, 0.02);
            if (type == Demo.COUCH) grow(b, new double[]{pr[1], pr[3] + 0.42}, 0.02);
            if (type == Demo.CHAIR) grow(b, new double[]{pr[1], pr[3] + 0.45}, 0.02);
        }
        b[1] = Math.min(b[1], -0.02);
        return b;
    }

    private static void grow(double[] b, double[] q, double m) {
        b[0] = Math.min(b[0], q[0] - m);
        b[1] = Math.min(b[1], q[1] - m);
        b[2] = Math.max(b[2], q[0] + m);
        b[3] = Math.max(b[3], q[1] + m);
    }

    static void draw(Canvas cv, Demo.Anim a, Demo.Pose pose, float w, float h, float dens, boolean label) {
        new Painter(cv, a, w, h, dens, label).frame(pose);
    }

    private static final class Painter {
        final Canvas cv;
        final Demo.Anim a;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final float w, h, dens;
        final boolean label;
        double scale, ox, oy;
        final int sx;
        final double[] bb;

        Painter(Canvas cv, Demo.Anim a, float w, float h, float dens, boolean label) {
            this.cv = cv; this.a = a; this.w = w; this.h = h; this.dens = dens; this.label = label;
            sx = a.mirror ? -1 : 1;
            double[] b = bounds(a);
            bb = b.clone();
            if (a.mirror) { double t = -b[2]; b[2] = -b[0]; b[0] = t; }
            float labelH = label ? 30 * dens : 6 * dens;
            double bw = b[2] - b[0], bh = b[3] - b[1];
            scale = Math.min((w - 16 * dens) / bw, (h - labelH - 10 * dens) / bh);
            ox = w / 2.0 - (b[0] + b[2]) / 2.0 * scale;
            oy = (h - labelH) / 2.0 + 4 * dens + (b[1] + b[3]) / 2.0 * scale;
        }

        float X(double x) { return (float) (ox + sx * x * scale); }
        float Y(double y) { return (float) (oy - y * scale); }
        float L(double m) { return (float) (m * scale); }

        void seg(double[] a1, double[] b1, double width, int color) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeWidth(L(width));
            p.setColor(color);
            cv.drawLine(X(a1[0]), Y(a1[1]), X(b1[0]), Y(b1[1]), p);
        }

        void glowSeg(double[] a1, double[] b1, double width, int color) {
            seg(a1, b1, width * 1.9, (color & 0x00FFFFFF) | 0x33000000);
            seg(a1, b1, width, color);
        }

        void dot(double[] c, double r, int color) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            cv.drawCircle(X(c[0]), Y(c[1]), L(r), p);
        }

        void glowDot(double[] c, double r, int color) {
            dot(c, r * 1.45, (color & 0x00FFFFFF) | 0x33000000);
            dot(c, r, color);
        }

        boolean on(int part) { return (a.parts & part) != 0; }

        void frame(Demo.Pose pose) {
            props();
            Demo.J j = Demo.joints(pose, a.view);
            if (a.view == Demo.SIDE) side(pose, j); else front(pose, j);
            if (label && !pose.label.isEmpty()) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(Ui.TEXT);
                p.setTypeface(Ui.HEAD);
                p.setTextAlign(Paint.Align.CENTER);
                float ts = 15 * dens;
                p.setTextSize(ts);
                while (p.measureText(pose.label) > w - 12 * dens && ts > 9 * dens) p.setTextSize(ts -= dens);
                cv.drawText(pose.label, w / 2f, h - 10 * dens, p);
            }
        }

        void props() {
            p.setStyle(Paint.Style.FILL);
            if (a.noFloor) { // seen from above or the front: an exercise mat behind her
                p.setColor(MAT);
                float l = Math.min(X(bb[0] + 0.04), X(bb[2] - 0.04)), r = Math.max(X(bb[0] + 0.04), X(bb[2] - 0.04));
                cv.drawRoundRect(new RectF(l, Y(bb[3] - 0.04), r, Y(bb[1] + 0.04)), 10 * dens, 10 * dens, p);
            }
            for (double[] pr : a.props) {
                int type = (int) pr[0];
                double x0 = Math.min(pr[1], pr[2]), x1 = Math.max(pr[1], pr[2]), y = pr[3];
                switch (type) {
                    case Demo.MAT: box(x0, 0, x1, 0.025, MAT); break;
                    case Demo.WALL: {
                        float x = X(pr[1]);
                        p.setColor(PROP);
                        float wd = 10 * dens;
                        float xx = sx * (a.view == Demo.FRONT ? 1 : 1) > 0 ? x : x - wd;
                        cv.drawRect(a.view == Demo.FRONT ? 0 : Math.min(xx, xx + wd), 0,
                            a.view == Demo.FRONT ? w : Math.max(xx, xx + wd), Y(0), p);
                        if (a.view == Demo.FRONT) { // the wall behind her
                            p.setColor(0xFF231A30);
                            cv.drawRect(0, 0, w, Y(0), p);
                        }
                        break;
                    }
                    case Demo.CHAIR: {
                        box(x0, y - 0.05, x1, y, PROP_EDGE);                     // seat
                        double back = a.mirror ? x1 : x0;
                        box(back - 0.03, y, back + 0.03, y + 0.45, PROP_EDGE);   // backrest
                        box(x0 + 0.02, 0, x0 + 0.06, y - 0.05, PROP);            // legs
                        box(x1 - 0.06, 0, x1 - 0.02, y - 0.05, PROP);
                        break;
                    }
                    case Demo.COUCH: {
                        box(x0, 0.06, x1, y, PROP);
                        box(x0, y - 0.07, x1, y, PROP_EDGE);                     // cushion
                        double back = x0 < -0.1 ? x0 : x1;
                        box(back - 0.02, 0.06, back + 0.16, y + 0.42, PROP);     // back
                        box(x0 + 0.03, 0, x0 + 0.08, 0.06, PROP_EDGE);
                        box(x1 - 0.08, 0, x1 - 0.03, 0.06, PROP_EDGE);
                        break;
                    }
                    case Demo.COUNTER: box(x0, 0, x1, y, PROP); box(x0 - 0.03, y - 0.04, x1, y, PROP_EDGE); break;
                    case Demo.STEP: box(x0, 0, x1, y, PROP); box(x0, y - 0.025, x1, y, PROP_EDGE); break;
                    case Demo.TABLE: {
                        box(x0, y - 0.05, x1, y, PROP_EDGE);
                        box(x0 + 0.03, 0, x0 + 0.08, y - 0.05, PROP);
                        box(x1 - 0.08, 0, x1 - 0.03, y - 0.05, PROP);
                        break;
                    }
                    case Demo.DOOR: { // a door frame seen from the side: the jamb she / he grips
                        box(x0, 0, x0 + 0.08, 2.0, PROP);
                        box(x0 - 0.01, 0, x0 + 0.01, 2.0, PROP_EDGE);
                        break;
                    }
                    case Demo.STAIRS: {
                        for (int i = 0; i < 5; i++) {
                            double sx0 = x0 + i * 0.26;
                            box(sx0, 0, x1, y * (i + 1), PROP);
                            box(sx0, y * (i + 1) - 0.025, x1, y * (i + 1), PROP_EDGE);
                        }
                        break;
                    }
                    default: break;
                }
            }
            if (!a.noFloor) {
                p.setColor(FLOOR);
                p.setStyle(Paint.Style.FILL);
                cv.drawRect(0, Y(0), w, Y(0) + 2.5f * dens, p);
            }
        }

        void box(double x0, double y0, double x1, double y1, int color) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            float l = Math.min(X(x0), X(x1)), r = Math.max(X(x0), X(x1));
            cv.drawRoundRect(new RectF(l, Y(y1), r, Y(y0)), 3 * dens, 3 * dens, p);
        }

        // ---- side view ----
        void side(Demo.Pose pose, Demo.J j) {
            // far limbs, darker
            arm(j.shF, j.elF, j.waF, true);
            leg(j.hipF, j.knF, j.anF, j.toF, true);
            torsoSide(pose, j);
            headSide(pose, j);
            leg(j.hipN, j.knN, j.anN, j.toN, false);
            arm(j.shN, j.elN, j.waN, false);
        }

        void arm(double[] s, double[] e, double[] wr, boolean far) {
            int c = on(Demo.ARM) ? (far ? HOT_FAR : HOT) : (far ? FAR : BODY);
            if (!far) { seg(s, e, 0.085 + 0.035, OUTLINE); seg(e, wr, 0.07 + 0.035, OUTLINE); dot(wr, 0.06, OUTLINE); }
            if (on(Demo.ARM)) { glowSeg(s, e, 0.085, c); glowSeg(e, wr, 0.07, c); }
            else { seg(s, e, 0.085, c); seg(e, wr, 0.07, c); }
            dot(wr, 0.042, far ? FAR : BODY);
        }

        void leg(double[] hp, double[] k, double[] an, double[] to, boolean far) {
            int th = on(Demo.THIGH) || on(Demo.INNER) ? (far ? HOT_FAR : HOT) : (far ? FAR : BODY);
            int sh = on(Demo.SHIN) ? (far ? HOT_FAR : HOT) : (far ? FAR : BODY);
            if (!far) { seg(hp, k, 0.15 + 0.03, OUTLINE); seg(k, an, 0.10 + 0.03, OUTLINE); seg(an, to, 0.07 + 0.03, OUTLINE); }
            if (th == HOT || th == HOT_FAR) glowSeg(hp, k, 0.15, th); else seg(hp, k, 0.15, th);
            if (sh == HOT || sh == HOT_FAR) glowSeg(k, an, 0.10, sh); else seg(k, an, 0.10, sh);
            seg(an, to, 0.07, far ? FAR : BODY);
        }

        void torsoSide(Demo.Pose pose, Demo.J j) {
            double[] back = Demo.dir(j.belly + 180);
            double[] mid = {(j.hip[0] + j.sh[0]) / 2 + back[0] * pose.bend * 2, (j.hip[1] + j.sh[1]) / 2 + back[1] * pose.bend * 2};
            boolean core = on(Demo.CORE), upper = on(Demo.BACK) || on(Demo.SHOULDER);
            // glutes
            double[] g = Demo.add(Demo.add(j.hip, 0.06, j.belly + 180), 0.015, pose.t);
            double gr = M ? 0.095 : 0.105;
            if (on(Demo.GLUTE) || on(Demo.HIPSIDE)) glowDot(g, gr, HOT); else dot(g, gr, BODY);
            Path path = new Path();
            path.moveTo(X(j.hip[0]), Y(j.hip[1]));
            path.quadTo(X(mid[0]), Y(mid[1]), X(j.sh[0]), Y(j.sh[1]));
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            if (core) {
                p.setStrokeWidth(L(0.22 * 1.6));
                p.setColor(0x33FF4FA3);
                cv.drawPath(path, p);
            }
            p.setStrokeWidth(L(M ? 0.25 : 0.22));
            p.setColor(core ? HOT : BODY);
            cv.drawPath(path, p);
            if (upper) { // upper back / shoulders band
                double[] u0 = lerpPt(j.hip, j.sh, 0.62), u1 = j.sh;
                seg(u0, u1, 0.20, HOT);
            }
            // chest
            double[] c = Demo.add(lerpPt(j.hip, j.sh, M ? 0.78 : 0.72), M ? 0.06 : 0.075, j.belly);
            double cr = M ? 0.085 : 0.07;
            if (on(Demo.CHEST)) glowDot(c, cr, HOT); else dot(c, cr, core ? HOT : BODY);
            if (on(Demo.HEART)) heart(Demo.add(lerpPt(j.hip, j.sh, 0.70), 0.02, j.belly), pose.t);
        }

        /** A small heart on the chest: the STAMINA moves. */
        void heart(double[] c, double t) {
            double r = 0.05;
            double[] up = Demo.dir(t), side = Demo.dir(t + 90);
            dot(c, r * 2.4, 0x33FF4FA3);
            double[] l = {c[0] + up[0] * r * 0.4 - side[0] * r * 0.55, c[1] + up[1] * r * 0.4 - side[1] * r * 0.55};
            double[] rr = {c[0] + up[0] * r * 0.4 + side[0] * r * 0.55, c[1] + up[1] * r * 0.4 + side[1] * r * 0.55};
            dot(l, r * 0.62, HOT);
            dot(rr, r * 0.62, HOT);
            double[] tip = {c[0] - up[0] * r * 1.3, c[1] - up[1] * r * 1.3};
            Path h = new Path();
            h.moveTo(X(l[0] - side[0] * r * 0.6), Y(l[1] - side[1] * r * 0.6));
            h.lineTo(X(rr[0] + side[0] * r * 0.6), Y(rr[1] + side[1] * r * 0.6));
            h.lineTo(X(tip[0]), Y(tip[1]));
            h.close();
            p.setStyle(Paint.Style.FILL);
            p.setColor(HOT);
            cv.drawPath(h, p);
        }

        void headSide(Demo.Pose pose, Demo.J j) {
            seg(j.sh, j.neck, 0.075, BODY);
            double hd = j.headDir;
            if (!M) {
                double[] tail = Demo.add(j.head, 0.115, hd + 120);
                seg(Demo.add(j.head, 0.06, hd + 100), tail, 0.06, HAIR);
                dot(tail, 0.045, HAIR);
            }
            dot(j.head, Demo.HEAD, HAIR);
            dot(Demo.add(j.head, 0.022, hd - 90), Demo.HEAD * 0.84, BODY);
            double[] eye = Demo.add(Demo.add(j.head, 0.055, hd - 90), 0.012, hd);
            if (pose.eyesClosed) seg(Demo.add(eye, 0.012, hd + 90), Demo.add(eye, 0.012, hd - 90), 0.012, INK);
            else dot(eye, 0.013, INK);
        }

        // ---- front view ----
        void front(Demo.Pose pose, Demo.J j) {
            if (pose.nearBehind) leg(j.hipN, j.knN, j.anN, j.toN, true);
            leg(j.hipF, j.knF, j.anF, j.toF, pose.nearBehind ? false : false);
            arm(j.shF, j.elF, j.waF, false);
            torsoFront(pose, j);
            if (!pose.nearBehind) leg(j.hipN, j.knN, j.anN, j.toN, false);
            arm(j.shN, j.elN, j.waN, false);
            headFront(pose, j);
        }

        void torsoFront(Demo.Pose pose, Demo.J j) {
            double hw = M ? 0.035 : 0.065;
            double[] hn = Demo.add(j.hipN, hw, pose.t - 90), hf = Demo.add(j.hipF, hw, pose.t + 90);
            Path path = new Path();
            path.moveTo(X(j.shN[0]), Y(j.shN[1]));
            path.lineTo(X(j.waistN[0]), Y(j.waistN[1]));
            path.lineTo(X(hn[0]), Y(hn[1]));
            path.lineTo(X(hf[0]), Y(hf[1]));
            path.lineTo(X(j.waistF[0]), Y(j.waistF[1]));
            path.lineTo(X(j.shF[0]), Y(j.shF[1]));
            path.close();
            p.setStyle(Paint.Style.FILL_AND_STROKE);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setStrokeWidth(L(0.06));
            boolean core = on(Demo.CORE);
            if (core) {
                p.setColor(0x33FF4FA3);
                p.setStrokeWidth(L(0.14));
                cv.drawPath(path, p);
                p.setStrokeWidth(L(0.06));
            }
            p.setColor(core ? HOT : BODY);
            cv.drawPath(path, p);
            if (on(Demo.GLUTE) && !core) { // lower torso / hips
                Path g = new Path();
                g.moveTo(X(j.waistN[0]), Y(j.waistN[1]));
                g.lineTo(X(hn[0]), Y(hn[1]));
                g.lineTo(X(hf[0]), Y(hf[1]));
                g.lineTo(X(j.waistF[0]), Y(j.waistF[1]));
                g.close();
                p.setColor(HOT);
                cv.drawPath(g, p);
            }
            if (on(Demo.HIPSIDE)) { glowDot(hn, 0.07, HOT); glowDot(hf, 0.07, HOT); }
            if (on(Demo.SHOULDER) || on(Demo.BACK)) { glowDot(j.shN, 0.07, HOT); glowDot(j.shF, 0.07, HOT); }
            if (on(Demo.CHEST)) seg(lerpPt(j.shN, j.waistN, 0.25), lerpPt(j.shF, j.waistF, 0.25), 0.10, HOT);
            if (on(Demo.HEART)) heart(add2(lerpPt(j.shN, j.waistN, 0.3), lerpPt(j.shF, j.waistF, 0.3)), pose.t);
        }

        void headFront(Demo.Pose pose, Demo.J j) {
            seg(j.sh, j.neck, 0.08, BODY);
            double hd = j.headDir;
            dot(Demo.add(j.head, M ? 0.02 : 0.012, hd), Demo.HEAD * (M ? 1.0 : 1.1), HAIR);
            dot(Demo.add(j.head, 0.018, hd + 180), Demo.HEAD * 0.86, BODY);
            for (int s = -1; s <= 1; s += 2) {
                double[] e = Demo.add(Demo.add(j.head, 0.035, hd + 90 * s), 0.0, hd);
                if (pose.eyesClosed) seg(Demo.add(e, 0.012, hd + 90), Demo.add(e, 0.012, hd - 90), 0.012, INK);
                else dot(e, 0.012, INK);
            }
        }

        static double[] add2(double[] a1, double[] b1) {
            return new double[]{(a1[0] + b1[0]) / 2, (a1[1] + b1[1]) / 2};
        }

        static double[] lerpPt(double[] a1, double[] b1, double u) {
            return new double[]{a1[0] + (b1[0] - a1[0]) * u, a1[1] + (b1[1] - a1[1]) * u};
        }
    }
}
