package application.alex.biriukov.loveread;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DownloadActivity extends AppCompatActivity {
    public static final String SITE = "http://loveread.ec/";
    EditText etAddress;
    Button btnSubmit, btnDownloads;
    ProgressDialog dialog;
    int maxPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        etAddress = (EditText) findViewById(R.id.etAddress);
        btnSubmit = (Button) findViewById(R.id.btnSubmit);
        btnDownloads = (Button) findViewById(R.id.btnExplorer);

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String address = "http://loveread.ec/read_book.php?id=" + etAddress.getText().toString() + "&p=1";
                new DownloadTask().execute(address);
            }
        });

        btnDownloads.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri selectedUri = Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath() + "/LoveRead");
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(selectedUri, "resource/folder");
                startActivity(intent);
            }
        });
    }

    public class DownloadTask extends AsyncTask<String, Void, Void>{

        @Override
        protected Void doInBackground(String... str) {
            String address = str[0];
            String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/LoveRead";
            File myfile = new File(fileName);
            FileWriter fw = null;

            Document doc;
            Elements elements;
            Element element;

            myfile.mkdir();

            //Создаем файл с красивым названием книги
            try {
                Document docTemt = Jsoup.connect(address).get();
                Elements tempElements = docTemt.select("td[class=tb_read_book]").get(0).select("a");
                fileName += "/" + tempElements.get(1).text() + " - " + tempElements.get(0).text() + ".txt";
                myfile = new File(fileName);
                fw = new FileWriter(myfile, true);
                //Узнаем количество страниц для диалогового окна
                tempElements = docTemt.select("div[class=navigation]").get(0).children();
                maxPage = Integer.parseInt(tempElements.get(tempElements.size() - 2).text());


            } catch (IOException e) {
                e.printStackTrace();
                //Если появляется ошибка то прекращаем работу корректно
                cancel(true);
            }

            if (isCancelled()) return null;
            publishProgress();

            while(true){
                try {
                    //Загружаем страницу
                    doc = Jsoup.connect(address).get();
                    elements = doc.select("p[class=MsoNormal], p[class=strong], p[class=em], div[class=take_h1]");

                    //Записываем контент в файл
                    for(Element el: elements){
                        if(el.text().length() > 0)
                            fw.append(el.text() + System.getProperty("line.separator"));
                    }
                    fw.flush();
                    publishProgress();

                    //Получаем адрес следующей страницы
                    elements = doc.select("div[class=navigation]").get(0).children();
                    element = elements.get(elements.size() - 1);
                    if((address = element.attr("href")).length() == 0) {
                        break;
                    }else {
                        address = SITE + address;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        //После загрузки нужно закрыть окно и вывести сообщение
        @Override
        protected void onPostExecute(Void voids) {
            super.onPostExecute(voids);
            dialog.cancel();
            dialog = null;
            Toast.makeText(getApplicationContext(), "Done!", Toast.LENGTH_LONG).show();
        }

        //После каждой загруженной страницы обновляем диалоговое окно
        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            if(dialog == null){
                dialog = new ProgressDialog(DownloadActivity.this);
                dialog.setMessage("Loading...");
                dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                dialog.setCancelable(false);
                dialog.setMax(maxPage);
                dialog.show();

                return;
            }
            dialog.incrementProgressBy(1);
        }

        @Override
        protected void onCancelled() {
            AlertDialog.Builder builder = new AlertDialog.Builder(DownloadActivity.this);
            builder.setTitle("Ошибка!")
                    .setMessage("Не удалось найти книгу с указанным id")
                    .setCancelable(true)
                    .setPositiveButton("Закрыть", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();

            super.onCancelled();
        }

    }


}
