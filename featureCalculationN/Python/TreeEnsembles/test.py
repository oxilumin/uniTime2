from sklearn.ensemble import RandomForestRegressor
from sklearn.ensemble import ExtraTreesRegressor
from sklearn.ensemble import AdaBoostRegressor
from sklearn.cross_validation import cross_val_predict
from sklearn.preprocessing import PolynomialFeatures

import matplotlib.pyplot as plt
import numpy as np
from sklearn import svm, datasets
from sklearn.metrics import precision_recall_curve
from sklearn.metrics import roc_curve
from sklearn.cross_validation import train_test_split
from sklearn.preprocessing import label_binarize
from sklearn.multiclass import OneVsRestClassifier

from numpy import genfromtxt

features = genfromtxt('C:\Data\OlyaMasterThesis\Instances\infoSetsNew1_LS.csv', delimiter=',')
results = genfromtxt('C:\Data\OlyaMasterThesis\Instances\infoSetsNew1_LS_Result.csv', delimiter=',')

features2 = []

for f in features:
    newFeatures = []
    for i in range(len(f)):
        for j in range(i, len(f)):
            newFeatures.append(f[i] * f[j])
            newFeatures.append((f[i]+0.1) / (f[j]+0.1))
            newFeatures.append(f[i] - f[j])
            newFeatures.append(f[i] + f[j])
            newFeatures.append(f[i])
    features2.append(newFeatures)

weights = results[:,3]
w = [t for t in results[:,2] if t > 0 ]
clf = ExtraTreesRegressor(n_estimators=2000, max_leaf_nodes=20, min_samples_leaf=10, n_jobs=6)
target = results[:,2]
scores = cross_val_predict(clf, features, target, cv=20)
v = np.column_stack((target, scores))
vs = v[v[:,1].argsort()]
tt = [t for t in vs[:,0] if t > 0]
totalones = tt.count(1)
total = len(vs[:,0])
alist = []
zeros = 0
ones = 0
for z in vs:
    if z[0] == 0:
        zeros += 1

    if z[0] == 1:
        ones += 1

    alist.append([(zeros+(totalones - ones))/total,z[1]])

al = np.array(alist)
baseline = totalones/total;
plt.clf()
plt.plot([0, 1], [baseline, baseline], 'k-', lw=2)
plt.plot([0, 1], [0.96, 0.96], 'k-', lw=2)
plt.plot(al[:,1], al[:,0], label='Accuracy curve')
plt.xlabel('Threshold')
plt.ylabel('Accuracy')
plt.ylim([0.0, 1.0])
plt.xlim([0.0, 1.0])
plt.legend(loc="lower left")
plt.show()
