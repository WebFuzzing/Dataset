import json
import pandas as pd
import numpy as np
import os
import joblib
import pymysql
from sqlalchemy import create_engine
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.utils import class_weight
from sklearn.metrics import confusion_matrix
from tensorflow.keras.models import Sequential, load_model
from tensorflow.keras.layers import Dense, Dropout, ReLU, Input
from tensorflow.keras.regularizers import l2
from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau

# Database connection details (from environment variables or passed as secure inputs)
DB_USERNAME = os.getenv('DB_USERNAME')
DB_PASSWORD = os.getenv('DB_PASSWORD')
DB_HOST = os.getenv('DB_HOST')
DB_NAME = os.getenv('DB_NAME')

# Function to fetch and format data
def fetch_and_format_data():
    try:
        engine = create_engine(f'mysql+pymysql://{DB_USERNAME}:{DB_PASSWORD}@{DB_HOST}/{DB_NAME}')
        with engine.connect() as connection:
            query = '''
                SELECT 
                o.id AS articleId, 
                s.articleCountScore,
                s.discrepancyDegreeYearScore,
                s.emailMatchScore,
                s.genderScoreIdentityArticleDiscrepancy,
                s.grantMatchScore,
                s.journalSubfieldScore,
                s.nameMatchFirstScore,
                s.nameMatchLastScore,
                s.nameMatchMiddleScore,
                s.nameMatchModifierScore,
                s.organizationalUnitMatchingScore,
                s.relationshipEvidenceTotalScore,
                s.relationshipNonMatchScore,
                s.scopusNonTargetAuthorInstitutionalAffiliationScore,
                s.targetAuthorInstitutionalAffiliationMatchTypeScore,
                s.pubmedTargetAuthorInstitutionalAffiliationMatchTypeScore,                  
                o.userAssertion
            FROM 
                feedback_score_total o 
            JOIN 
                scoring_identity s 
            ON 
                s.pmid = o.pmid 
                AND s.personIdentifier = o.personIdentifier
            JOIN 
                scoring_overall q
            ON 
                q.pmid = o.pmid 
                AND q.personIdentifier = o.personIdentifier                

            WHERE 
                o.userAssertion in ('ACCEPTED','REJECTED')

            ORDER BY RAND();
            '''
            data = pd.read_sql(query, connection)
            print("Data fetched successfully!")

        # Save data to 'identityOnlyScoringInput.json' (optional, can be removed if not needed)
        json_data = data.to_dict(orient='records')
        with open('identityOnlyScoringInput.json', 'w') as f:
            json.dump(json_data, f, indent=4)
        print("Data saved to 'identityOnlyScoringInput.json'")

        return data

    except Exception as e:
        print(f"Error retrieving or formatting data: {e}")
        sys.exit(1)

# Function to preprocess data
def preprocess_data(data):
    # Drop NaN values
    data = data.dropna()

    # Define features and target
    feature_columns = [ 
        'articleCountScore','discrepancyDegreeYearScore','emailMatchScore','genderScoreIdentityArticleDiscrepancy',
        'grantMatchScore','journalSubfieldScore','nameMatchFirstScore','nameMatchLastScore','nameMatchMiddleScore',
        'nameMatchModifierScore','organizationalUnitMatchingScore','relationshipEvidenceTotalScore',
        'relationshipNonMatchScore','scopusNonTargetAuthorInstitutionalAffiliationScore',
        'targetAuthorInstitutionalAffiliationMatchTypeScore','pubmedTargetAuthorInstitutionalAffiliationMatchTypeScore'
    ]
    X = data[feature_columns]
    y = data['userAssertion']

    # Encode labels
    label_encoder = LabelEncoder()

    # Encode labels manually
    y_encoded = y.map({'ACCEPTED': 1, 'REJECTED': 0}).values

    # Normalize features and save scaler
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)
    joblib.dump(scaler, 'identityOnlyScaler.save')
    print("Scaler saved as 'identityOnlyScaler.save'")

    return X_scaled, y_encoded, feature_columns, label_encoder, data

# Function to build and train the model
def build_and_train_model(X_scaled, y_encoded):
    # Split data
    X_train, X_test, y_train, y_test = train_test_split(X_scaled, y_encoded, test_size=0.2, random_state=42)

    # Calculate class weights
    class_weights = class_weight.compute_class_weight(class_weight='balanced', classes=np.unique(y_train), y=y_train)
    class_weights_dict = dict(enumerate(class_weights))

    # Build the model
    model = Sequential()
    model.add(Input(shape=(X_train.shape[1],)))
    model.add(Dense(64))
    model.add(ReLU(negative_slope=0.01))  # Updated argument
    model.add(Dropout(0.2))
    model.add(Dense(8))
    model.add(ReLU(negative_slope=0.01))  # Updated argument
    model.add(Dropout(0.2))
    model.add(Dense(1, activation='sigmoid'))

    # Compile the model
    model.compile(loss='binary_crossentropy', optimizer='adam', metrics=['accuracy'])

    # Callbacks
    early_stopping = EarlyStopping(monitor='val_loss', patience=40, restore_best_weights=True)
    reduce_lr = ReduceLROnPlateau(monitor='val_loss', factor=0.2, patience=10, min_lr=1e-6)

    # Train the model
    model.fit(X_train, y_train, epochs=100, batch_size=10, validation_split=0.1,
              class_weight=class_weights_dict, callbacks=[early_stopping, reduce_lr])

    # Evaluate the model
    _, accuracy = model.evaluate(X_test, y_test)
    print(f'Neural Network Accuracy: {accuracy*100:.2f}%')

    # Save the model
    model.save('identityOnlyModel.keras')
    print('Model saved as "identityOnlyModel.keras"')

    return model, X_test, y_test

# Function to generate predictions (without saving to files)
def generate_predictions(model, X_scaled, data):
    # Make predictions (probabilities between 0 and 1)
    predictions = model.predict(X_scaled).flatten()

    # Combine predictions with article IDs
    results = pd.DataFrame({
        'articleId': data['articleId'],
        'predicted_score': predictions
    })

    # Optionally, print some of the results to verify
    print("\nSample Predictions:")
    print(results.head())

    # If you need to use the predictions further in your code, you can return 'results'
    return results

# Main function
if __name__ == "__main__":
    # Fetch and format data
    data = fetch_and_format_data()

    # Preprocess data
    X_scaled, y_encoded, feature_columns, label_encoder, data = preprocess_data(data)

    # Build and train the model
    model, X_test, y_test = build_and_train_model(X_scaled, y_encoded)

    # Generate predictions
    predictions = generate_predictions(model, X_scaled, data)