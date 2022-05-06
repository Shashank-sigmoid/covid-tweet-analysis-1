// Total no. of donations received on a country basis

// Source => https://covidfunding.eiu.com/api-docs/

// Fetching the data in json form from the above API source and pushing the data into donations collection in MongoDB

// Top 10 donation sources all around the world with their donation amount and frequency
db.donations.aggregate([
{ $group: { _id: "$source", Count: { $sum: 1 }, Total: { $sum: "$amount" } } },
{ $sort: { Total: -1 } },
{ $limit: 10 }
])