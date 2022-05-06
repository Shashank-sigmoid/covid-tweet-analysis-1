// Top 100 words occurring on tweets involving corona virus

db.covid_tweets.aggregate([
  { $project: { words: { $split: ["$text", " "] } } },
  { $unwind: "$words" },
  { $match : { words: { $nin: ["a", "I", "are", "is", "to", "the", "of", "and", "RT"]} } },
  { $group: { _id: "$words" , total: { "$sum": 1 } } },
  { $sort: { total : -1 } },
  { $limit: 100 }
])