package na.eraka.JSONutils.sortPictogramsUtils;

import na.eraka.utils.JSONutils;

import org.json.JSONArray;
import org.json.JSONException;

public class  SortPictograms extends SortObjects{
    final static int RUN = 32;

    /**
     * Eraka 28 Oct 2026 (speed): sorts by picture id with Java's own sort. OTTAA's quicksort
     * took the last element as pivot, which is the worst case for a list that is already
     * sorted (as the pictures file always is): about n^2/2 = 600,000 JSON reads for 1,100
     * pictures, every time the files were loaded. Java's sort is n log n, and almost free for
     * a list that is already in order.
     */
    @Override
    public void quickSort(JSONArray array, int low, int high) throws JSONException {
        if (array == null || low >= high) return;
        java.util.List<org.json.JSONObject> part = new java.util.ArrayList<>(high - low + 1);
        for (int i = low; i <= high; i++) part.add(array.getJSONObject(i));
        java.util.Collections.sort(part, (a, b) -> Integer.compare(a.optInt("id", -1), b.optInt("id", -1)));
        for (int i = low; i <= high; i++) array.put(i, part.get(i - low));
    }


    /* This function takes last element as pivot, places
       the pivot element at its correct position in sorted
       array, and places all smaller (smaller than pivot)
       to left of pivot and all greater elements to right
       of pivot */



    @Override
    protected int partition(JSONArray arr, int low, int high) throws JSONException {
        int pivot = JSONutils.getId(arr.getJSONObject(high));
        // Index of smaller element and
        // indicates the right position
        // of pivot found so far
        int i = (low - 1);
        for(int j = low; j <= high - 1; j++)
        {
            if (JSONutils.getId(arr.getJSONObject(j))< pivot)
            {
                i++;
                swap(arr, i, j);
            }
        }
        swap(arr, i + 1, high);
        return (i + 1);
    }

    /* The main function that implements QuickSort
                  arr[] --> Array to be sorted,
                  low --> Starting index,
                  high --> Ending index
         */


    public  void heapSort(JSONArray array) throws JSONException {
        int n = array.length();
        for (int i = n / 2 - 1; i >= 0; i--)
            heapify(array, n, i);
        for (int i = n - 1; i > 0; i--) {
            // Move current root to end
            swap(array,0,i);
            // call max heapify on the reduced heap
            heapify(array, i, 0);
        }
    }
    private  void heapify(JSONArray array,int n,int i) throws JSONException {
        int largest = i;
        int left = 2*i+1;
        int right = 2*i+2;
        if(left<n && JSONutils.getId(array.getJSONObject(left))>JSONutils.getId(array.getJSONObject(largest))){
            largest = left;
        }
        if(right<n && JSONutils.getId(array.getJSONObject(right))>JSONutils.getId(array.getJSONObject(largest))){
            largest = right;
        }
        if(largest != i){
            swap(array,i,largest);
            heapify(array,n,largest);
        }

    }

    private  void insertionSort(JSONArray array, int left, int right) throws JSONException {
        for (int i = left + 1; i <= right; i++)
        {
            int temp = JSONutils.getId(array.getJSONObject(i));
            int j = i - 1;
            while (JSONutils.getId(array.getJSONObject(j)) > temp && j >= left)
            {
                swap(array,j+1,j);
                j--;
            }
            swap(array,j+1,i);
        }
    }


}
