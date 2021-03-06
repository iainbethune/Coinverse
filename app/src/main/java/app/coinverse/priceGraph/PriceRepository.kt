package app.coinverse.priceGraph

import android.util.Log
import androidx.lifecycle.MutableLiveData
import app.coinverse.firebase.contentEthBtcCollection
import app.coinverse.priceGraph.models.*
import app.coinverse.utils.DateAndTime.getTimeframe
import app.coinverse.utils.Exchange
import app.coinverse.utils.Exchange.*
import app.coinverse.utils.TIMESTAMP
import app.coinverse.utils.Timeframe
import app.coinverse.utils.awaitRealtime
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query.Direction.ASCENDING
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import io.reactivex.Observable
import io.reactivex.Observable.just
import io.reactivex.Observable.zip
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function4
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers.io
import kotlinx.coroutines.tasks.await

private val LOG_TAG = PriceRepository::class.java.simpleName

/**
 * TODO: Remove price graphs and replace with content search bar.
 */
object PriceRepository {

    val compositeDisposable = CompositeDisposable()
    var graphLiveData = MutableLiveData<HashMap<Exchange, PriceGraphData>>()
    var priceDifferenceDetailsLiveData = MutableLiveData<PercentDifference>()
    var graphConstraintsLiveData = MutableLiveData<PriceGraphXAndYConstraints>()

    private var xIndex = 0.0
    private var index = 0
    private var minY: Double = 1000000000000.0
    private var maxY: Double = -1000000000000.0
    private var minX: Double = 1000000000000.0
    private var maxX: Double = -1000000000000.0

    private var exchangeOrdersPointsMap = HashMap<Exchange, ExchangeOrdersDataPoints>()
    private var exchangeOrdersDataMap = HashMap<Exchange, PriceGraphData>()

    suspend fun getPrices(isRealtime: Boolean, isOnCreateCall: Boolean, timeframe: Timeframe) {
        if (isRealtime) {
            if (isOnCreateCall) {
                exchangeOrdersPointsMap.clear()
                exchangeOrdersDataMap.clear()
                index = 0
            }
            val response = contentEthBtcCollection
                    .orderBy(TIMESTAMP, ASCENDING)
                    .whereGreaterThan(TIMESTAMP, getTimeframe(timeframe))
                    .awaitRealtime()
            if (response.error == null)
                response?.let { value -> parsePriceData(value.packet!!.documentChanges) }
            else Log.e(LOG_TAG, "Price Data EventListener Failed.", response.error)
        } else {
            try {
                exchangeOrdersPointsMap.clear()
                exchangeOrdersDataMap.clear()
                index = 0
                parsePriceData(contentEthBtcCollection.orderBy(TIMESTAMP, ASCENDING)
                        .whereGreaterThan(TIMESTAMP, getTimeframe(timeframe))
                        .get().await().documentChanges)
            } catch (error: FirebaseFirestoreException) {
                Log.e(LOG_TAG, "Price Data EventListener Failed.", error)
            }
        }
    }

    private fun parsePriceData(documentChanges: List<DocumentChange>) {
        documentChanges.map { priceDataDocument ->
            xIndex = index++.toDouble()
            priceDataDocument.document.toObject(MaximumPercentPriceDifference::class.java).also { priceData ->
                priceDifferenceDetailsLiveData.value = priceData.percentDifference
                //TODO: Refactor axis to use dates.
                compositeDisposable.add(zip(
                        generateGraphData(COINBASE, priceData.coinbaseExchangeOrderData).subscribeOn(io()),
                        generateGraphData(BINANCE, priceData.binanceExchangeOrderData).subscribeOn(io()),
                        generateGraphData(GEMINI, priceData.geminiExchangeOrderData).subscribeOn(io()),
                        generateGraphData(KRAKEN, priceData.krakenExchangeOrderData).subscribeOn(io()),
                        getFunction4())
                        .subscribeOn(io())
                        .observeOn(mainThread())
                        .subscribeWith(object : DisposableObserver<List<HashMap<Exchange, PriceGraphData>>>() {
                            override fun onNext(priceGraphDataList: List<HashMap<Exchange, PriceGraphData>>) {
                                priceGraphDataList.map { graphLiveData.postValue(it) }
                            }

                            override fun onError(e: Throwable) {
                                e.printStackTrace()
                            }

                            override fun onComplete() {
                                compositeDisposable.clear()
                            }
                        }))
            }
        }
    }

    private fun generateGraphData(exchange: Exchange, exchangeOrderData: ExchangeOrderData)
            : Observable<HashMap<Exchange, PriceGraphData>> {
        val baseToQuoteBid = exchangeOrderData.baseToQuoteBid.price
        val baseToQuoteAsk = exchangeOrderData.baseToQuoteAsk.price
        val bidDataPoint = DataPoint(xIndex, baseToQuoteBid)
        val askDataPoint = DataPoint(xIndex, baseToQuoteAsk)
        if (!exchangeOrdersPointsMap.containsKey(exchange)
                && !exchangeOrdersDataMap.containsKey(exchange)) {
            val bidDataPoints = arrayListOf(bidDataPoint)
            val askDataPoints = arrayListOf(askDataPoint)
            exchangeOrdersPointsMap[exchange] = ExchangeOrdersDataPoints(bidDataPoints, askDataPoints)
            exchangeOrdersDataMap[exchange] =
                    PriceGraphData(
                            LineGraphSeries(bidDataPoints.toTypedArray()),
                            LineGraphSeries(askDataPoints.toTypedArray()))
        } else {
            val bidDataPoints = exchangeOrdersPointsMap[exchange]?.bidsLiveData
            bidDataPoints?.add(bidDataPoint)
            val askDataPoints = exchangeOrdersPointsMap[exchange]?.asksLiveData
            askDataPoints?.add(askDataPoint)
            exchangeOrdersDataMap[exchange]?.bids = LineGraphSeries(bidDataPoints?.toTypedArray())
            exchangeOrdersDataMap[exchange]?.asks = LineGraphSeries(askDataPoints?.toTypedArray())
        }
        findMinAndMaxGraphConstraints(baseToQuoteBid, baseToQuoteAsk)
        return just(exchangeOrdersDataMap)
    }

    private fun getFunction4(): Function4<HashMap<Exchange, PriceGraphData>, HashMap<Exchange,
            PriceGraphData>, HashMap<Exchange, PriceGraphData>, HashMap<Exchange, PriceGraphData>,
            List<HashMap<Exchange, PriceGraphData>>> {
        return Function4 { coinbasePriceGraphData: HashMap<Exchange, PriceGraphData>,
                           binancePriceGraphData: HashMap<Exchange, PriceGraphData>,
                           geminiPriceGraphData: HashMap<Exchange, PriceGraphData>,
                           krakenPriceGraphData: HashMap<Exchange, PriceGraphData> ->
            listOf(coinbasePriceGraphData, binancePriceGraphData, geminiPriceGraphData,
                    krakenPriceGraphData)
        }
    }

    private fun findMinAndMaxGraphConstraints(baseToQuoteBid: Double, baseToQuoteAsk: Double) {
        findMinAndMaxY(Math.max(baseToQuoteBid, baseToQuoteAsk))
        findMinAndMaxX(xIndex)
        graphConstraintsLiveData.postValue(PriceGraphXAndYConstraints(minX, maxX, minY, maxY))
    }

    private fun findMinAndMaxX(xIndex: Double) {
        if (xIndex < minX) minX = xIndex
        else if (xIndex > maxX) maxX = xIndex
    }

    private fun findMinAndMaxY(averageWeightedPrice: Double?) {
        if (averageWeightedPrice!! < minY) minY = averageWeightedPrice
        else if (averageWeightedPrice > maxY) maxY = averageWeightedPrice
    }
}