Compare the detection result with the oracle and calculate precision, recall.
Output the result that merges the detection result and the oracle as a CSV file.
Basically, run this script after running detection for oracle data.

```bash
python3 -m venv venv
source venv/bin/activate
python3 -m src.main -i -d ../detection-result/0123/universal.csv -o ./result_0123.csv
# or venv/bin/python
```

```bash
python3 -m unittest
```
