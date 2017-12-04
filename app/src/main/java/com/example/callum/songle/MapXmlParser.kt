package com.example.callum.songle

import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Matrix
import java.net.URL
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth




class MapXmlParser {
    //We don't use namespaces
    private val ns: String? = null

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(input : InputStream):ArrayList<Pair<Placemark,Bitmap?>>{
        Log.d("MYAPP","IN PARSE!")
        input.use {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,false)
            parser.setInput(input,null)
            parser.nextTag()
            return readKml(parser)
        }
    }

    @Throws(XmlPullParserException::class,IOException::class)
    private fun readKml(parser: XmlPullParser): ArrayList<Pair<Placemark,Bitmap?>>{
        Log.d("MYAPP","IN READKML!")
        parser.require(XmlPullParser.START_TAG,ns,"kml")
        parser.nextTag()
        val placemarkers = readDocument(parser)
        parser.require(XmlPullParser.END_TAG,ns,"kml")
        Log.d("MYAPP","LEAVING READKML!")
        return placemarkers
    }

    @Throws(XmlPullParserException::class,IOException::class)
    private fun readDocument(parser: XmlPullParser): ArrayList<Pair<Placemark,Bitmap?>>{
        Log.d("MYAPP","IN READDOCUMENTS!")
        val placemarkers = ArrayList<Pair<Placemark,Bitmap?>>()
        //The key is the style id and the pair is they scale and icon
        val styles = HashMap<String,Bitmap>()
        parser.require(XmlPullParser.START_TAG,ns,"Document")
        while(parser.next()!=XmlPullParser.END_TAG){
            if(parser.eventType!=XmlPullParser.START_TAG){
                continue
            }
            //Starts by looking for the Style tag
            if(parser.name == "Style"){
                val triple = readStyle(parser)
                val url = URL(triple.third)
                val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                val width = bitmap.width
                val height = bitmap.height
                //Creat a matrix for the resize of the bitmap
                val matrix = Matrix()
                //Initialize the matrix
                matrix.postScale(triple.second.toFloat(), triple.second.toFloat())

                //Resize the bitmap
                val resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false)
                bitmap.recycle()
                styles.put(triple.first, resizedBitmap)
            }
            //Then look for placemarkers
            else if (parser.name == "Placemark"){
                val triple = readPlacemark(parser)
                //The second element in the triple is the description this should match one of the
                //keys in the hashmap to give the placemarkers icon and scale
                //FileName defualts to empty string
                val placemarker = Placemark("",triple.first, triple.third,"")
                val pair = Pair(placemarker,styles.get(triple.second))
                placemarkers.add(pair)
            }
            else{
                skip(parser)
            }
        }
        Log.d("MYAPP","LEAVING READDOCUMENTS!")
        parser.nextTag()
        return placemarkers
    }

    @Throws(XmlPullParserException::class,IOException::class)
    private fun readStyle(parser: XmlPullParser): Triple<String,Double,String>{
        Log.d("MYAPP","IN READSTYLE!")
        var id = ""
        parser.require(XmlPullParser.START_TAG,ns,"Style")
        if(parser.name == "Style"){
            id = parser.getAttributeValue(null,"id")
            parser.nextTag()
        }
        val pair = readIconStyle(parser)
        parser.require(XmlPullParser.END_TAG,ns,"Style")
        Log.d("MYAPP","LEAVING READSTYLE!")
        return Triple(id,pair.first,pair.second)
    }

    @Throws(XmlPullParserException::class,IOException::class)
    private fun readIconStyle(parser: XmlPullParser): Pair<Double,String>{
        Log.d("MYAPP","IN READICONSTYLE!")
        parser.require(XmlPullParser.START_TAG,ns,"IconStyle")
        parser.nextTag()
        var scale =readScale(parser)
        parser.nextTag()
        var icon =readIcon(parser)
        parser.require(XmlPullParser.END_TAG,ns,"IconStyle")
        parser.nextTag()
        Log.d("MYAPP","LEAVING READICONSTYLE!")
        return Pair(scale,icon)
    }

    @Throws(XmlPullParserException::class,IOException::class)
    private fun readScale(parser: XmlPullParser): Double{
        Log.d("MYAPP","IN READSCALE!")
        parser.require(XmlPullParser.START_TAG, ns, "scale")
        val scale = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "scale")
        Log.d("MYAPP","LEAVING READSCALE!")
        return scale.toDouble()
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        Log.d("MYAPP","IN READTEXT!")
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        Log.d("MYAPP","LEAVING READTEXT!")
        return result
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readIcon(parser: XmlPullParser): String {
        Log.d("MYAPP","IN READICON!")
        parser.require(XmlPullParser.START_TAG, ns, "Icon")
        parser.nextTag()
        val icon = readHref(parser)
        parser.require(XmlPullParser.END_TAG, ns, "Icon")
        parser.nextTag()
        Log.d("MYAPP","LEAVING READICON!")
        return icon
    }

    @Throws(XmlPullParserException::class,IOException::class)
    private fun readHref(parser: XmlPullParser): String{
        Log.d("MYAPP","IN READHREF!")
        parser.require(XmlPullParser.START_TAG, ns, "href")
        val href = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "href")
        parser.nextTag()
        Log.d("MYAPP","LEAVING READHREF!")
        return href
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readPlacemark(parser: XmlPullParser): Triple<String,String,Pair<Double,Double>> {
        Log.d("MYAPP","IN READPLACEMARK!")
        parser.require(XmlPullParser.START_TAG, ns, "Placemark")
        var name = ""
        var description = ""
        var point = Pair(0.0,0.0)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG){
                continue
            }
            when(parser.name){
                "name" -> name = readName(parser)
                "description" -> description = readDescription(parser)
                "Point" -> point = readPoint(parser)
                else -> skip(parser)
            }
        }
        Log.d("MYAPP","LEAVING READPLACEMARK!")
        return Triple(name, description, point)
    }

    @Throws(XmlPullParserException::class,IOException::class)
    private fun readName(parser: XmlPullParser): String{
        Log.d("MYAPP","IN READNAME!")
        parser.require(XmlPullParser.START_TAG, ns, "name")
        val name = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "name")
        Log.d("MYAPP","LEAVING READNAME!")
        return name
    }

    @Throws(XmlPullParserException::class,IOException::class)
    private fun readDescription(parser: XmlPullParser): String{
        Log.d("MYAPP","IN READDESCRIPTION!")
        parser.require(XmlPullParser.START_TAG, ns, "description")
        val description = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "description")
        Log.d("MYAPP","LEAVING READDESCRIPTION!")
        return description
    }

    @Throws(XmlPullParserException::class,IOException::class)
    private fun readPoint(parser: XmlPullParser): Pair<Double,Double>{
        Log.d("MYAPP","IN READPOINT!")
        parser.require(XmlPullParser.START_TAG,ns,"Point")
        parser.nextTag()
        val point = readCoordinates(parser)
        parser.require(XmlPullParser.END_TAG,ns,"Point")
        Log.d("MYAPP","LEAVING READPOINT!")
        return point
    }

    @Throws(XmlPullParserException::class,IOException::class)
    private fun readCoordinates(parser: XmlPullParser): Pair<Double,Double>{
        Log.d("MYAPP","IN READCOORD!")
        parser.require(XmlPullParser.START_TAG, ns, "coordinates")
        //Get the coordinates as a string
        val string = readText(parser)
        //Split the coordinate string into a list of two string
        val stringList = string.split(",")
        val coordinates = Pair(stringList[0].toDouble(),stringList[1].toDouble())
        parser.require(XmlPullParser.END_TAG, ns, "coordinates")
        parser.nextTag()
        Log.d("MYAPP","LEAVING READCOORD!")
        return coordinates
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        Log.d("MYAPP","IN SKIP!")
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
        Log.d("MYAPP","LEAVING SKIP!")
    }
}