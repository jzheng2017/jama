package nl.jiankai.operators;

import nl.jiankai.ElementTransformationTracker;

public class SignatureUtil {

    public static String getFirstSignature(String signature, ElementTransformationTracker tracker) {
        if (signature.contains("#")) {
            String[] split = signature.split("#");
            String firstSignature = tracker.currentSignature(split[0]);
            signature = firstSignature + "#" + split[1];

            return signature;
        } else {
            return tracker.currentSignature(signature);
        }
    }
}
