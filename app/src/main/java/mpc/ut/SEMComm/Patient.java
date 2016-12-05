package mpc.ut.SEMComm;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Aurelius on 12/3/16.
 */

@JsonObject
public class Patient implements Serializable {

    /*
     * Annotate a field that you want sent with the @JsonField marker.
     */
    @JsonField
    public String name;

    @JsonField
    public Integer heartRate;

    @JsonField
    public ArrayList<Integer> systolicArray;

    @JsonField
    public ArrayList<Integer> diastolicArray;

    @JsonField
    public Boolean shareHeart;

    @JsonField
    public Boolean shareBlood;

    @JsonField
    public Boolean locationSensitive;

    /*
     * Note that since this field isn't annotated as a
     * @JsonField, LoganSquare will ignore it when parsing
     * and serializing this class.
     */
    public int nonJsonField;

    public void setBloodPressure() {
        systolicArray = new ArrayList<>();
        diastolicArray = new ArrayList<>();

        systolicArray.add(111);
        systolicArray.add(118);
        systolicArray.add(125);
        systolicArray.add(138);
        systolicArray.add(142);
        systolicArray.add(144);
        systolicArray.add(141);
        systolicArray.add(139);
        systolicArray.add(138);
        systolicArray.add(140);

        diastolicArray.add(72);
        diastolicArray.add(77);
        diastolicArray.add(81);
        diastolicArray.add(89);
        diastolicArray.add(96);
        diastolicArray.add(94);
        diastolicArray.add(90);
        diastolicArray.add(84);
        diastolicArray.add(88);
        diastolicArray.add(92);
    }
}
