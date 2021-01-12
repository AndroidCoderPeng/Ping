package com.css.jyjt.ping.utils;

import com.css.jyjt.ping.ResultBean;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class PingHelper {
    /**
     * 每秒ping一次目的IP
     */
    public static ResultBean ping(String address) {
        ResultBean resultBean = new ResultBean();
        StringBuilder builder = new StringBuilder();
        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        try {
            process = runtime.exec("ping -c 1 -w 1 " + address);
            InputStreamReader streamReader = new InputStreamReader(process.getInputStream());
            BufferedReader reader = new BufferedReader(streamReader);
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            String s = builder.toString();
            if (s.equals("")) {
                return null;
            }
            int index = s.indexOf("bytes of data.");
            resultBean.setTitle(s.substring(0, index + 14));
            resultBean.setContent(s.substring(index + 15));
            return resultBean;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
            runtime.gc();
        }
        return null;
    }
}
