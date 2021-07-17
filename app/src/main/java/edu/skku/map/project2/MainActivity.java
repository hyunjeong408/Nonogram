package edu.skku.map.project2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    GridView container, containerL, containerT;
    EditText keyword;
    int cellSize, maxL, maxT, alpha, h;
    Bitmap bitmap;//orgBitmap
    Bitmap resizedB;
    Bitmap picGrayScale;
    Bitmap[] picFin = new Bitmap[400]; //이게 정답이자... 찐 뷰
    Bitmap[] initPic = new Bitmap[400];
    Bitmap[] toInit = new Bitmap[400];
    MyBaseAdapter adapter, adapterL, adapterT;
    //ImageView orgImg;
    int[] numArrL = new int[200];
    int[] numArrT = new int[200];
    int[] setL;
    int[] setT;
    int[] checkBlack = new int[400];
    int[] ansBlock = new int[400];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        keyword = (EditText)findViewById(R.id.editText);
        //orgImg = (ImageView) findViewById(R.id.imgView);
        container = (GridView) findViewById(R.id.gridImg);
        containerL = (GridView) findViewById(R.id.gridTxtLeft);
        containerT = (GridView) findViewById(R.id.gridTxtTop);
        Button searchBtn = (Button)findViewById(R.id.button1);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( keyword.getText().toString().length() != 0 ) {
                    //get bitmap
                    String txt = keyword.getText().toString();
                    ApiExamSearch thread = null;
                    try {
                        thread = new ApiExamSearch(txt);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    thread.start();
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    resizedB = resizeBitmap(bitmap);
                    //orgImg.setImageBitmap(resizedB);

                    changeToBnW(resizedB);
                    //picGrayScale = resizedB;
                    //orgImg2.setImageBitmap(resizedB);

                    int picH = resizedB.getHeight() / 20;
                    int picW = resizedB.getWidth() / 20;
                    int k = 0;
                    for(int c = 0; c < 400; c++){
                        ansBlock[c] = 0;
                    }
                    for(int i = 0; i < 20; i++){
                        for(int j = 0; j < 20 ; j++){
                            picGrayScale = Bitmap.createBitmap(resizedB, j*picW, i*picH, picW, picH);
                            changeToGray(picGrayScale, i, j);
                            picFin[k] = picGrayScale;
                            k++;
                        }
                    }
                    k=0;
                    for(int i = 0; i < 20; i++){
                        for(int j = 0; j < 20 ; j++){
                            picGrayScale = Bitmap.createBitmap(resizedB, j*picW, i*picH, picW, picH);
                            makeInit(picGrayScale);
                            initPic[k] = picGrayScale;
                            k++;
                        }
                    }
                    //toInit = initPic;
                    //흰 그리드뷰 보여주기. initPic

                    adapter = new MyBaseAdapter(MainActivity.this, initPic);
                    container.setAdapter(adapter);

                    checkCol();
                    checkRow();
                    maxL++; maxT++;
                    setL = new int[maxL*20];
                    setT = new int[maxT*20];

                    for(int i=0; i<20; i++){
                        for(int j = 0; j<maxT; j++){
                            setT[20*j+i]=numArrT[10*i+j];
                        }
                    }

                    for(int i=0; i<20; i++){
                        for(int j = 0; j<maxL; j++){
                            setL[maxL*i+j]=numArrL[10*i+j];
                        }
                    }
                    adapterL = new MyBaseAdapter(MainActivity.this, setL, 1);

                    //containerL.setLayoutParams(new GridView.LayoutParams(cellSize, cellSize));
                    containerL.setColumnWidth(cellSize);
                    containerL.setNumColumns(maxL);
                    containerL.setAdapter(adapterL);

                    adapterT = new MyBaseAdapter(MainActivity.this, setT, 1);
                    containerT.setColumnWidth(cellSize);
                    containerT.setAdapter(adapterT);
                    container.setOnItemClickListener(new GridViewClickListener());

                }
                else{
                    Toast.makeText(MainActivity.this, "Enter a query to search.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button galleryBtn = (Button)findViewById(R.id.button2);
        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgThread thread2 = null;
                thread2 = new imgThread();
                thread2.start();
                try {
                    thread2.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        });

    }

    class imgThread extends Thread {
        @Override
        public void run() {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        InputStream in;
        Bitmap tmp;
        if(requestCode == 1){
            if(resultCode == RESULT_OK && data != null){
                try{
                    Uri uri = data.getData();
                    tmp = MediaStore.Images.Media.getBitmap(getContentResolver(),uri);
                    int nh = (int) (tmp.getHeight() * (1024.0 / tmp.getWidth()));
                    bitmap = Bitmap.createScaledBitmap(tmp, 1024, nh, true);
                    resizedB = resizeBitmap(bitmap);
                    //orgImg.setImageBitmap(resizedB);
                    for(int c = 0; c < 400; c++){
                        ansBlock[c] = 0;
                    }
                    changeToBnW(resizedB);

                    int picH = resizedB.getHeight() / 20;
                    int picW = resizedB.getWidth() / 20;
                    int k = 0;
                    for(int i = 0; i < 20; i++){
                        for(int j = 0; j < 20 ; j++){
                            picGrayScale = Bitmap.createBitmap(resizedB, j*picW, i*picH, picW, picH);
                            changeToGray(picGrayScale, i, j);
                            picFin[k] = picGrayScale;
                            k++;
                        }
                    }
                    k=0;
                    for(int i = 0; i < 20; i++){
                        for(int j = 0; j < 20 ; j++){
                            picGrayScale = Bitmap.createBitmap(resizedB, j*picW, i*picH, picW, picH);
                            makeInit(picGrayScale);
                            initPic[k] = picGrayScale;
                            k++;
                        }
                    }
                    adapter = new MyBaseAdapter(MainActivity.this, initPic);
                    container.setAdapter(adapter);

                    checkCol();
                    checkRow();
                    maxL++;
                    maxT++;
                    setL = new int[maxL*20];
                    setT = new int[maxT*20];

                    for(int i=0; i<20; i++){
                        for(int j = 0; j<maxT; j++){
                            setT[20*j+i]=numArrT[10*i+j];
                        }
                    }

                    for(int i=0; i<20; i++){
                        for(int j = 0; j<maxL; j++){
                            setL[maxL*i+j]=numArrL[10*i+j];
                        }
                    }
                    adapterL = new MyBaseAdapter(MainActivity.this, setL, 1);

                    //containerL.setLayoutParams(new GridView.LayoutParams(cellSize, cellSize));
                    containerL.setColumnWidth(cellSize);
                    containerL.setNumColumns(maxL);
                    containerL.setAdapter(adapterL);

                    adapterT = new MyBaseAdapter(MainActivity.this, setT, 1);
                    containerT.setColumnWidth(cellSize);
                    containerT.setAdapter(adapterT);
                    container.setOnItemClickListener(new GridViewClickListener());

                }catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void checkCol(){ //T
        int k = 0;
        maxT = 0;
        int bf = 0;//bf가 1이면 이전에 black, 0이면 이전이 white
        for(int i=0; i<20; i++){//i번째 줄 탐색
            for(int j = 0; j < 20; j++){
                if(j == 0){//first line
                    if(checkBlack[i] == 1){//black이면
                        numArrT[10*i+k] = 1;
                        bf = 1;
                    }
                    else{
                        numArrT[10*i+k] = 0;
                        bf = 0;
                    }
                }
                else{ //첫칸이 아니면
                    if(checkBlack[j*20+i] == 1){
                        if(bf == 1){//계속 black이라는 거임.
                            numArrT[10*i+k]++;
                        }
                        else{
                            if(numArrT[10*i+k] != 0){
                                k++;
                            }
                            numArrT[10*i+k] = 1;
                        }
                    }
                    bf = checkBlack[j*20+i];
                }
            }
            if(k>maxT && k<10){ maxT = k;}
            k++;
            while(k<10){
                numArrT[10*i+k] = 0;
                k++;
            }
            k=0;
        }

    }

    void checkRow(){
        int k = 0;
        maxL = 0;
        int bf = 0;//bf가 1이면 이전에 black, 0이면 이전이 white
        for(int i=0; i<20; i++){//i번째 줄 탐색
            for(int j = 0; j < 20; j++){
                if(j == 0){//first line
                    if(checkBlack[20*i] == 1){//black이면
                        numArrL[10*i+k] = 1;
                        bf = 1;
                    }
                    else{
                        numArrL[10*i+k] = 0;
                        bf = 0;
                    }
                }
                else{ //첫칸이 아니면
                    if(checkBlack[20*i+j] == 1){
                        if(bf == 1){//계속 black이라는 거임.
                            numArrL[10*i+k]++;
                        }
                        else{
                            if(numArrL[10*i+k] != 0){
                                k++;
                            }
                            numArrL[10*i+k] = 1;
                        }
                    }
                    bf = checkBlack[20*i+j];
                }
            }
            if(k>maxL && k<10){ maxL = k; }
            k++;
            while(k<10){
                numArrL[10*i+k] = 0;
                k++;
            }
            k=0;
        }
    }



    void changeToBnW(Bitmap bm){
        Bitmap bitmap = bm;
        int reW = bitmap.getWidth();
        int reH = bitmap.getHeight();
        //int[] pixels = new int[reW*reH];
        int p;
        Bitmap tmp = Bitmap.createBitmap(reW, reH, Bitmap.Config.ARGB_8888);
        //bitmap.getPixels(pixels, 0, reW, 0, 0, reW, reH);
        int a, r, g, b;
        for (int x = 0; x < reW; x++) {
            for(int y = 0; y < reH; y++) {
                p = bitmap.getPixel(x, y);
                a= Color.alpha(p);
                r = Color.red(p);
                g = Color.green(p);
                b = Color.blue(p);
                int gray = (int) (r * 0.2126 + g * 0.7152 + b * 0.0722);
                if (gray > 128)
                    gray = 255;
                else
                    gray = 0;
                resizedB.setPixel(x, y, Color.argb(a, gray, gray, gray));
            }

            //pixels[x] = ((gray&0x0ff)<<16)|((gray&0x0ff)<<8)|(gray&0x0ff);
        }
        //resizedB.setPixels(pixels, 0, reW, 0, 0, reW, reH);
    }

    void changeToGray(Bitmap bm, int i, int j){
        Bitmap bitmap = bm;
        int reW = bitmap.getWidth();
        int reH = bitmap.getHeight();
        int avg = 0;
        int gray = 255;
        //int[] pixels = new int[reW*reH];
        int p;
        Bitmap tmp = Bitmap.createBitmap(reW, reH, Bitmap.Config.ARGB_8888);
        //bitmap.getPixels(pixels, 0, reW, 0, 0, reW, reH);
        int a = 0;
        int r, g, b;
        for (int x = 0; x < reW; x++) {
            for(int y = 0; y < reH; y++) {
                p = bitmap.getPixel(x, y);
                a= Color.alpha(p);
                r = Color.red(p);
                g = Color.green(p);
                b = Color.blue(p);
                gray = (int) (r * 0.2126 + g * 0.7152 + b * 0.0722);
                avg += gray;
                //resizedB.setPixel(x, y, Color.argb(a, gray, gray, gray));
            }
        }
        if((int)(avg / (reW*reH)) > 128){
            gray = 255;
            checkBlack[20*i+j] = 0;
        }
        else{
            gray = 0;
            checkBlack[20*i+j] = 1;
        }
        alpha = a;
        for (int x = 0; x < reW; x++) {
            for(int y = 0; y < reH; y++) {
                picGrayScale.setPixel(x, y, Color.argb(a, gray, gray, gray));
            }
        }
    }

    void toBlack(Bitmap bm){ //맞으면 검은 블럭으로 바꿔야지지
        int reW = bm.getWidth();
        int reH = bm.getHeight();
        int gray = 0;
        int a = alpha;
        for (int x = 0; x < reW; x++) {
            for(int y = 0; y < reH; y++) {
                picGrayScale.setPixel(x, y, Color.argb(a, gray, gray, gray));
            }
        }
    }

    void makeInit(Bitmap bm){
        Bitmap bitmap = bm;
        int reW = bitmap.getWidth();
        int reH = bitmap.getHeight();
        int p, a;
        for (int x = 0; x < reW; x++) {
            for(int y = 0; y < reH; y++) {
                p = bitmap.getPixel(x, y);
                a= alpha;
                picGrayScale.setPixel(x, y, Color.argb(a, 255, 255, 255));
            }
        }
    }

    Bitmap resizeBitmap(Bitmap b){
        Context context = MainActivity.this;
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        Display dis = wm.getDefaultDisplay();
        Point size = new Point();
        dis.getSize(size);
        int disW = (int)((size.x - 20)/5*4);
        h = (int)(disW / 20);
        cellSize = disW;
        return Bitmap.createScaledBitmap(b, disW, disW, true);
    }

    class MyBaseAdapter extends BaseAdapter {
        Context c;
        Bitmap items[];
        int itemsT[];
        int bool;

        MyBaseAdapter(Context c, Bitmap arr[]) {
            this.c = c;
            items = arr;
            bool = 0;
        }
        MyBaseAdapter(Context c, int arr[], int b){
            this.c = c;
            itemsT = arr;
            bool = b;
        }

        @Override
        public int getCount() {
            if(bool == 0){
                return items.length;
            }
            else{
                return itemsT.length;
            }

        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            LayoutInflater inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if(bool == 0) {
                ViewHolder holder;
                if (view == null) {
                    holder = new ViewHolder();
                    view = inflater.inflate(R.layout.image, viewGroup, false);
                    holder.img = (ImageView) view.findViewById(R.id.gridImage);
                    view.setTag(holder);
                } else {
                    holder = (ViewHolder) view.getTag();
                }

                holder.img.setImageBitmap(items[i]);
            }
            if(bool == 1){
                ViewHolderT holder;
                int ht = (int)(h - 1);
                if (view == null) {
                    holder = new ViewHolderT();
                    view = inflater.inflate(R.layout.text, viewGroup, false);
                    holder.txt = (TextView) view.findViewById(R.id.gridTxt);
                    holder.txt.setHeight(h);
                    view.setTag(holder);
                } else {
                    holder = (ViewHolderT) view.getTag();
                }
                String str = String.valueOf(itemsT[i])+" ";
                holder.txt.setHeight(h);
                holder.txt.setText(str);
            }
            return view;
        }

        private class ViewHolder {
            private ImageView img;
        }
        private class ViewHolderT{
            private TextView txt;
        }
    }

    class GridViewClickListener implements AdapterView.OnItemClickListener{
        Context c = MainActivity.this;
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //지금 클릭한 그리드뷰 포지션.
            //이걸 답 bitmap arr랑 비교해서
            // if 검은색 위치가 맞으면 지금 그리드뷰를 검은색으로 업데이트하고,
            //      업데이트 후 if 답 bitmap arr랑 지금 그리드뷰에 업데이트 된 arr랑 같으면 정답처리.
            // else 다시 초기 그리드뷰로 업데이트 & Toast 메세지
            int picH = resizedB.getHeight() / 20;
            int picW = resizedB.getWidth() / 20;
            if(checkBlack[position] == 1){
                if(ansBlock[position] == 0) {
                    int i = position / 20;
                    int j = position % 20;
                    picGrayScale = Bitmap.createBitmap(resizedB, j * picW, i * picH, picW, picH);
                    toBlack(initPic[position]);
                    initPic[position] = picGrayScale;
                    adapter = new MyBaseAdapter(MainActivity.this, initPic);
                    container.setAdapter(adapter);
                    ansBlock[position] = 1;
                    if (checkAns()) {
                        Toast.makeText(MainActivity.this, "FINISH!", Toast.LENGTH_LONG).show();
                    }
                }
            }
            else{
                int k = 0;
                int c = 0;
                for(int i = 0; i < 20; i++){
                    for(int j = 0; j < 20 ; j++){
                        picGrayScale = Bitmap.createBitmap(resizedB, j*picW, i*picH, picW, picH);
                        makeInit(picGrayScale);
                        initPic[k] = picGrayScale;
                        k++;
                        ansBlock[c] = 0; c++;
                    }
                }
                adapter = new MyBaseAdapter(MainActivity.this, initPic);
                container.setAdapter(adapter);
                Toast.makeText(MainActivity.this, "WRONG! RESTART.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    boolean checkAns(){
        for(int i = 0; i<400; i++){
            if(checkBlack[i] != ansBlock[i])
                return false;
        }
        return true;
    }

    class ApiExamSearch extends Thread{
        String text;
        String keyword;
        //Bitmap b;
        String clientId = "wb9dzTT6SL4hwIlldnUq"; //애플리케이션 클라이언트 아이디값"
        String clientSecret = "UPlIuO7pxN"; //애플리케이션 클라이언트 시크릿값"
        public ApiExamSearch(String str) throws UnsupportedEncodingException {
            this.keyword = str;
        }

        @Override
        public void run(){
            try {
                text = URLEncoder.encode(keyword, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("검색어 인코딩 실패",e);
            }

            String apiURL = "https://openapi.naver.com/v1/search/image?query=" + text + "&display=1" + "&start=1";
            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("X-Naver-Client-Id", clientId);
            requestHeaders.put("X-Naver-Client-Secret", clientSecret);
            String url = get(apiURL,requestHeaders);

            String json = url;
            String imgUrl = null;
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(url);
                JSONArray jsonArray = jsonObject.getJSONArray("items");

                JSONObject item = jsonArray.getJSONObject(0);
                imgUrl = item.getString("link");

            } catch (JSONException e) {
                e.printStackTrace();
            }
            bitmap = getImgBitmap(imgUrl);
        }


        private String get(String apiUrl, Map<String, String> requestHeaders){
            HttpURLConnection con = connect(apiUrl);
            try {
                con.setRequestMethod("GET");
                for(Map.Entry<String, String> header :requestHeaders.entrySet()) {
                    con.setRequestProperty(header.getKey(), header.getValue());
                }


                int responseCode = con.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) { // 정상 호출
                    return readBody(con.getInputStream());
                } else { // 에러 발생
                    return readBody(con.getErrorStream());
                }
            } catch (IOException e) {
                throw new RuntimeException("API 요청과 응답 실패", e);
            } finally {
                con.disconnect();
            }
        }
        private HttpURLConnection connect(String apiUrl){
            try {
                URL url = new URL(apiUrl);
                return (HttpURLConnection)url.openConnection();
            } catch (MalformedURLException e) {
                throw new RuntimeException("API URL이 잘못되었습니다. : " + apiUrl, e);
            } catch (IOException e) {
                throw new RuntimeException("연결이 실패했습니다. : " + apiUrl, e);
            }
        }

        private String readBody(InputStream body){
            InputStreamReader streamReader = new InputStreamReader(body);


            try (BufferedReader lineReader = new BufferedReader(streamReader)) {
                StringBuilder responseBody = new StringBuilder();


                String line;
                while ((line = lineReader.readLine()) != null) {
                    responseBody.append(line);
                }


                return responseBody.toString();
            } catch (IOException e) {
                throw new RuntimeException("API 응답을 읽는데 실패했습니다.", e);
            }
        }

        private Bitmap getImgBitmap(String url){
            Bitmap tmpB = null;
            BufferedInputStream bufS = null;
            HttpURLConnection imgCon = null;
            try {
                imgCon = connect(url);
                imgCon.connect();
                int imgSize = imgCon.getContentLength();
                bufS = new BufferedInputStream(imgCon.getInputStream(), imgSize);
                tmpB = BitmapFactory.decodeStream(bufS);
            }
            catch (Exception e){
                e.printStackTrace();
            } finally {
                if(bufS != null){
                    try{
                        bufS.close();
                    } catch (IOException e){}
                }
                if(imgCon != null){
                    imgCon.disconnect();
                }
            }
            return tmpB;
        }
    }

}