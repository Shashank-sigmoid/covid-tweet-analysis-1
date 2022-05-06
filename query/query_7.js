// Ranking of impacted countries over the last 2 months

db.cases_data.aggregate([
{ $group : { _id: "$location", total: { $sum: { $add : [ $deaths, "$confirmed" ] } } } },
{ $sort: { total: -1 } }
])