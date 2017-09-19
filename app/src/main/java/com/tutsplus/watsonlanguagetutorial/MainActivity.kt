package com.tutsplus.watsonlanguagetutorial

import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import com.github.kittinunf.fuel.httpGet
import com.ibm.watson.developer_cloud.document_conversion.v1.DocumentConversion
import com.ibm.watson.developer_cloud.language_translator.v2.LanguageTranslator
import com.ibm.watson.developer_cloud.language_translator.v2.model.Language
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.NaturalLanguageUnderstanding
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        documentURL.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_GO) {
                val url:String = documentURL.text.toString()
                url.httpGet().responseString { _, _, result ->
                    val (document, _) = result
                    if (document != null) {
                        val documentConverter = DocumentConversion(
                                DocumentConversion.VERSION_DATE_2015_12_01,
                                resources.getString(R.string.document_conversion_username),
                                resources.getString(R.string.document_conversion_password)
                        )

                        val tempFile = File.createTempFile("temp_file", null)
                        tempFile.writeText(document, Charsets.UTF_8)

                        AsyncTask.execute {
                            val plainText = documentConverter
                                                .convertDocumentToText(tempFile, "text/html")
                                                .execute()
                            runOnUiThread {
                                documentContents.text = plainText
                            }
                        }
                    }
                }
            }
            false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.my_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if(item?.itemId == R.id.action_translate) {
            val translator = LanguageTranslator(
                    resources.getString(R.string.language_translator_username),
                    resources.getString(R.string.language_translator_password)
            )

            AsyncTask.execute {
                val translatedDocument = translator
                                            .translate(documentContents.text.toString(),
                                            Language.GERMAN, Language.ENGLISH)
                                            .execute()
                runOnUiThread {
                    documentContents.text = translatedDocument.firstTranslation
                }
            }
        }

        if(item?.itemId == R.id.action_analyze) {
            val analyzer = NaturalLanguageUnderstanding(
                    NaturalLanguageUnderstanding.VERSION_DATE_2017_02_27,
                    resources.getString(R.string.natural_language_understanding_username),
                    resources.getString(R.string.natural_language_understanding_password)
            )

            val entityOptions = EntitiesOptions.Builder()
                    .emotion(true)
                    .sentiment(true)
                    .build()

            val sentimentOptions = SentimentOptions.Builder()
                    .document(true)
                    .build()

            val features = Features.Builder()
                    .entities(entityOptions)
                    .sentiment(sentimentOptions)
                    .build()

            val analyzerOptions = AnalyzeOptions.Builder()
                    .text(documentContents.text.toString())
                    .features(features)
                    .build()

            AsyncTask.execute {
                val results = analyzer.analyze(analyzerOptions).execute()

                val overallSentimentScore = results.sentiment.document.score
                var overallSentiment = "Positive"
                if(overallSentimentScore < 0.0)
                    overallSentiment = "Negative"
                if(overallSentimentScore == 0.0)
                    overallSentiment = "Neutral"

                var output = "Overall sentiment: ${overallSentiment}\n\n"

                for(entity in results.entities) {
                    output += "${entity.text} (${entity.type})\n"
                    val validEmotions = arrayOf("Anger", "Joy", "Disgust", "Fear", "Sadness")
                    val emotionValues = arrayOf(
                            entity.emotion.anger,
                            entity.emotion.joy,
                            entity.emotion.disgust,
                            entity.emotion.fear,
                            entity.emotion.sadness
                    )
                    val currentEmotion = validEmotions[emotionValues.indexOf(emotionValues.max())]
                    output += "Emotion: ${currentEmotion}, " +
                            "Sentiment: ${entity.sentiment.score}" +
                            "\n\n"
                }

                runOnUiThread {
                    documentContents.text = output
                }
            }
        }
        return true
    }
}
