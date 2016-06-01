from sklearn.ensemble import RandomForestRegressor
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.ensemble import ExtraTreesRegressor
from sklearn.ensemble import AdaBoostRegressor
from sklearn.tree import DecisionTreeRegressor
from sklearn.cross_validation import cross_val_predict
from sklearn.preprocessing import PolynomialFeatures

from sklearn.feature_selection import SelectFromModel

import matplotlib.pyplot as plt
import numpy as np
from sklearn import svm, datasets
from sklearn.metrics import precision_recall_curve
from sklearn.metrics import roc_curve
from sklearn.cross_validation import train_test_split
from sklearn.preprocessing import label_binarize
from sklearn.multiclass import OneVsRestClassifier

from numpy import genfromtxt

features = genfromtxt('C:\Data\OlyaMasterThesis\Instances\infoSetsNew1_LS2.csv', delimiter=',')
results = genfromtxt('C:\Data\OlyaMasterThesis\Instances\infoSetsNew1_LS_Result.csv', delimiter=',')

features2 = []

for f in features:
    newFeatures = []
    for i in range(len(f)):
        for j in range(i, len(f)):
            newFeatures.append(f[i] * f[j])
            newFeatures.append((f[i] + 0.1) / (f[j] + 0.1))
            newFeatures.append(f[i] - f[j])
            newFeatures.append(f[i] + f[j])
            newFeatures.append(f[i])
    features2.append(newFeatures)

target = results[:, 2]
weights = results[:, 3]

w = [t for t in results[:, 2] if t > 0]
clf = GradientBoostingRegressor(learning_rate=0.08, n_estimators=20, max_depth=40, min_samples_leaf=20)
clfR = GradientBoostingRegressor(learning_rate=0.08, n_estimators=120, max_depth=40, min_samples_leaf=20)
clffit = clf.fit(features2, target)
featuresSelectionModel = SelectFromModel(clffit)
features3 = featuresSelectionModel.fit_transform(features2, target)

features4 = []

for f in features3:
    newFeatures = []
    for i in range(len(f)):
        for j in range(i, len(f)):
            newFeatures.append(f[i] * f[j])
            newFeatures.append((f[i] + 0.2) / (f[j] + 0.2))
            newFeatures.append(f[i] - f[j])
            newFeatures.append(f[i] + f[j])
            newFeatures.append(f[i])
    features4.append(newFeatures)

clffit2 = clf.fit(features4, target)
featuresSelectionModel2 = SelectFromModel(clffit2)
features5 = featuresSelectionModel2.fit_transform(features4, target)

features6 = []

for f in features5:
    newFeatures = []
    for i in range(len(f)):
        for j in range(i, len(f)):
            newFeatures.append(f[i] * f[j])
            newFeatures.append((f[i] + 0.333) / (f[j] + 0.333))
            newFeatures.append(f[i] - f[j])
            newFeatures.append(f[i] + f[j])
            newFeatures.append(f[i])
    features6.append(newFeatures)

clffit3 = clf.fit(features6, target)
featuresSelectionModel3 = SelectFromModel(clffit3)
features7 = featuresSelectionModel3.fit_transform(features6, target)

scores = cross_val_predict(clfR, features7, target, cv=20)
v = np.column_stack((target, scores))
vs = v[v[:, 1].argsort()]
tt = [t for t in vs[:, 0] if t > 0]
totalones = tt.count(1)
total = len(vs[:, 0])
alist = []
zeros = 0
ones = 0
for z in vs:
    if z[0] == 0:
        zeros += 1

    if z[0] == 1:
        ones += 1

    alist.append([(zeros + (totalones - ones)) / total, z[1]])

al = np.array(alist)
baseline = totalones / total;
bestSolution = max(al[:, 0])
plt.clf()
plt.plot([0, 1], [baseline, baseline], 'k-', lw=2)
plt.plot([0, 1], [0.96, 0.96], 'k-', lw=2)
plt.plot(al[:, 1], al[:, 0], label='Accuracy curve')
plt.xlabel('Threshold')
plt.ylabel('Accuracy')
plt.ylim([0.0, 1.0])
plt.xlim([0.0, 1.0])
plt.legend(loc="lower left")
resultString = "%s / %s" % (baseline, bestSolution)
plt.text(0.3, 0.2, resultString)

plt.show()
