// Impact on economy, global analysis, trend analysis of a country

// Source => https://data.nasdaq.com/data/FRED-federal-reserve-economic-data/documentation

// Fetching GDP (Gross Domestic Product) of each country and pushing the data into global_economy collection in MongoDB

// Top 10 countries with all time highest GDP
db.global_economy.aggregate([
{ $group: { _id: "$Country", total: { $sum: "$GDP"} } },
{ $sort: { total: -1 } },
{ $limit: 10 }
])

// Top 10 countries with the highest GDP in a given year
db.global_economy.aggregate([
{ $match: { "Date": ISODate("2000-01-01") } },
{ $sort: { "GDP": -1 } },
{ $limit: 10 }
])

// Top 10 countries with the highest GDP between two given years
db.global_economy.aggregate([
{ $match: { "Date": { $gt: ISODate("2000-01-01"), $lt: ISODate("2020-01-01") } } },
{ $group: { _id: "$Country", total: { $sum: "$GDP" } } },
{ $sort: { total: -1 } },
{ $limit: 10 }
])