package revpay.util;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import revpay.model.Transaction;

public class ExportUtil {

    public static String exportTransactionsToCsv(List<Transaction> txs, String filePrefix) {
        FileWriter fw = null;
        try {
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String file = filePrefix + "_" + ts + ".csv";

            fw = new FileWriter(file);
            fw.write("TXN_ID,FROM_USER_ID,TO_USER_ID,AMOUNT,TYPE,STATUS,NOTE,REF_ID,CREATED_AT\n");

            for (Transaction t : txs) {
                fw.write(s(t.getTransactionId()) + ","
                        + s(t.getFromUserId()) + ","
                        + s(t.getToUserId()) + ","
                        + s(String.format("%.2f", t.getAmount())) + ","
                        + s(t.getType()) + ","
                        + s(t.getStatus()) + ","
                        + s(clean(t.getNote())) + ","
                        + s(t.getRefId()) + ","
                        + s(t.getCreatedAt()) + "\n");
            }
            fw.flush();
            return file;

        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {
            try { if (fw != null) fw.close(); } catch (Exception e) {}
        }
    }

    private static String s(Object o) {
        if (o == null) return "";
        String v = String.valueOf(o);
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            v = v.replace("\"", "\"\"");
            return "\"" + v + "\"";
        }
        return v;
    }

    private static String clean(String v) {
        if (v == null) return null;
        return v.replace("\n", " ").replace("\r", " ");
    }
}