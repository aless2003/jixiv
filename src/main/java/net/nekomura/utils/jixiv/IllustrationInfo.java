package net.nekomura.utils.jixiv;

import net.nekomura.utils.jixiv.Enums.artwork.PixivIllustrationType;
import net.nekomura.utils.jixiv.Enums.artwork.PixivImageSize;
import net.nekomura.utils.jixiv.Utils.FormatUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Objects;

public class IllustrationInfo extends ArtworkInfo {
    public IllustrationInfo(int id, JSONObject preloadData) {
        super(id, preloadData);
    }

    /**
     * Get Response Count
     * @return Response Count
     */
    public int getResponseCount() {
        return getPreloadData().getJSONObject("illust").getJSONObject(String.valueOf(getId())).getInt("responseCount");
    }

    private String getImageUrl(int page, @NotNull PixivImageSize type) {
        String pageZero = getPreloadData().getJSONObject("illust").getJSONObject(String.valueOf(getId())).getJSONObject("urls").getString(type.toString().toLowerCase());
        return pageZero.replace(getId() + "_p0", getId() + "_p" + page);
    }

    private String getUgoiraZipUrl() throws ParseException {
        Calendar calendar = getCreateDateCalendar();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);

        String monthString = FormatUtils.addZeroChar(month + 1);
        String dayString = FormatUtils.addZeroChar(day);
        String hourString = FormatUtils.addZeroChar(hour);
        String minuteString = FormatUtils.addZeroChar(minute);
        String secondString = FormatUtils.addZeroChar(second);

        return String.format("https://i.pximg.net/img-zip-ugoira/img/%d/%s/%s/%s/%s/%s/%d_ugoira1920x1080.zip",
                year,
                monthString,
                dayString,
                hourString,
                minuteString,
                secondString,
                getId());
    }

    /**
     * 獲取該插畫作品之第一頁圖片
     * @return 該插畫作品之第一頁圖片的byte array
     * @throws IOException 獲取失敗
     */
    public byte[] getImage() throws IOException {
        return getImage(0);
    }

    /**
     * 獲取該插畫作品之指定頁圖片
     * @param page 指定頁碼
     * @return 該插畫作品之指定頁圖片的byte array
     * @throws IOException 獲取失敗
     */
    public byte[] getImage(int page) throws IOException {
        return getImage(page, PixivImageSize.Original);
    }

    /**
     * 獲取該插畫作品之指定頁圖片
     * @param page 指定頁碼
     * @param size 圖片大小
     * @return 該插畫作品之指定頁圖片的byte array
     * @throws IOException 獲取失敗
     */
    public byte[] getImage(int page, PixivImageSize size) throws IOException {
        if (page > getPageCount() - 1)
            throw new IllegalArgumentException("The page is greater than the max page of the artwork .");

        OkHttpClient okHttpClient = new OkHttpClient();
        Request.Builder rb = new Request.Builder().url(getImageUrl(page, size));
        rb.addHeader("Referer", "https://www.pixiv.net/artworks");
        rb.method("GET", null);

        Response res = okHttpClient.newCall(rb.build()).execute();
        return Objects.requireNonNull(res.body()).bytes();
    }

    /**
     * 獲取該插畫動圖作品之所有幀之圖片之壓縮檔
     * @return 該插畫動圖作品之所有幀之圖片之壓縮檔的byte array
     * @throws ParseException 獲取動圖url失敗
     * @throws IOException 獲取失敗
     */
    public byte[] getUgoiraZip() throws ParseException, IOException {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request.Builder rb = new Request.Builder().url(getUgoiraZipUrl());
        rb.addHeader("Referer", "https://www.pixiv.net/artworks");
        rb.method("GET", null);

        Response res = okHttpClient.newCall(rb.build()).execute();
        return Objects.requireNonNull(res.body()).bytes();
    }

    /**
     * 獲取插畫作品類別
     * @return 插畫作品類別
     */
    public PixivIllustrationType getIllustrationType() {
        int typeNumber = getPreloadData().getJSONObject("illust").getJSONObject(String.valueOf(getId())).getInt("illustType");
        switch (typeNumber) {
            case 0:
                return PixivIllustrationType.Illustration;
            case 1:
                return PixivIllustrationType.Manga;
            case 2:
                return PixivIllustrationType.Ugoira;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * 獲取該插畫作品之圖片格式
     * @param page 指定頁碼
     * @return 插畫作品之圖片格式
     */
    public String getImageFileFormat(int page) {
        String[] filename = getImageUrl(page, PixivImageSize.Original).split("\\.");
        return filename[filename.length - 1];
    }

    /**
     * 下載插畫圖片指定頁碼
     * @param pathname 儲存位置
     * @param page 指定頁碼
     * @param size 圖片大小
     * @throws IOException 獲取失敗
     */
    public void download(String pathname, int page, PixivImageSize size) throws IOException {
        File file = new File(pathname);
        byte[] image = getImage(page, size);
        if (file.getParentFile() != null)
            file.getParentFile().mkdirs();
        file.createNewFile();
        FileOutputStream out = new FileOutputStream(file);
        out.write(image);
        out.close();
    }

    /**
     * 下載插畫圖片指定頁碼
     * @param pathname 儲存位置
     * @param page 指定頁碼
     * @throws Exception 下載失敗
     */
    public void download(String pathname, int page) throws Exception {
        download(pathname, page, PixivImageSize.Original);
    }

    /**
     * 下載插畫首張圖片
     * @param pathname 儲存位置
     * @throws Exception 下載失敗
     */
    public void download(String pathname) throws Exception {
        download(pathname, 0, PixivImageSize.Original);
    }

    /**
     * 下載插畫所有頁圖片
     * @param folder 儲存資料夾
     * @param size 圖片大小
     * @throws IOException 下載失敗
     */
    public void downloadAll(File folder, PixivImageSize size) throws IOException {
        int pageCount = getPageCount();
        folder.mkdirs();

        for (int i = 0; i < pageCount; i++) {
            File file = new File(String.format("%s/%d_p%d.%s", folder, getId(), i, getImageFileFormat(i)));
            byte[] image = getImage(i, size);
            FileOutputStream out = new FileOutputStream(file);
            out.write(image);
            out.close();
        }
    }

    /**
     * 下載插畫所有頁圖片
     * @param folderPath 儲存資料夾位置
     * @param type 圖片大小
     * @throws IOException 下載失敗
     */
    public void downloadAll(String folderPath, PixivImageSize type) throws IOException {
        downloadAll(new File(folderPath), type);
    }

    /**
     * 下載該插畫動圖作品之所有幀之圖片之壓縮檔
     * @param pathname 儲存位置
     * @throws Exception 下載失敗
     */
    public void downloadUgoiraZip(String pathname) throws Exception {
        if (!getIllustrationType().equals(PixivIllustrationType.Ugoira)) {
            throw new IllegalArgumentException("The Illustration is not an ugoira.");
        }

        File file = new File(pathname);
        byte[] bytes = getUgoiraZip();
        if (file.getParentFile() != null)
            file.getParentFile().mkdirs();
        file.createNewFile();
        FileOutputStream out = new FileOutputStream(file);
        out.write(bytes);
        out.close();
    }
}